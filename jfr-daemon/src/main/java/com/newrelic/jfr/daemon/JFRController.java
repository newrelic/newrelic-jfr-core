package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;
import static com.newrelic.jfr.daemon.EnvironmentVars.ENV_APP_NAME;
import static com.newrelic.jfr.daemon.JFRUploader.COMMON_ATTRIBUTES;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.telemetry.TelemetryClient;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JFRController {
  private static final Logger logger = LoggerFactory.getLogger(JFRController.class);

  private static final int DEFAULT_PORT = 1099;
  private static final String DEFAULT_HOST = "localhost";

  private final JFRJMXRecorder recorder;
  private final JFRUploader uploader;
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          2,
          r -> {
            Thread result = new Thread(r, "JFRController");
            result.setDaemon(true);
            return result;
          });

  private final boolean streamFromJmx = true;
  private volatile boolean shutdown = false;

  public JFRController(JFRUploader uploader, JFRJMXRecorder recorder) {
    this.recorder = recorder;
    this.uploader = uploader;
  }

  // This needs to be exposed to JMX / k8s
  public void shutdown() {
    shutdown = true;
  }

  void setup() {
    try {
      recorder.startRecording();
    } catch (Exception e) {
      e.printStackTrace();
      shutdown();
      throw new RuntimeException(e);
    }
  }

  void loop(Duration harvestInterval) throws IOException {
    while (!shutdown) {
      try {
        TimeUnit.MILLISECONDS.sleep(harvestInterval.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // Ignore the premature return and trigger the next JMX dump at once
      }
      try {
        final var pathToFile =
            streamFromJmx ? recorder.streamRecordingToFile() : recorder.copyRecordingToFile();

        executorService.submit(() -> uploader.handleFile(pathToFile));
      } catch (MalformedObjectNameException
          | MBeanException
          | InstanceNotFoundException
          | ReflectionException e) {
        logger.error("JMX streaming failed: ", e);
      } catch (OpenDataException e) {
        logger.error("Open data exception: ", e);
      }
    }
    executorService.shutdown();
  }

  public static void main(String[] args) {
    // FIXME Handle config
    var host = DEFAULT_HOST;
    var port = DEFAULT_PORT;

    try {
      DaemonConfig config = DaemonConfig.builder()
              .jmxHost(host)
              .jmxPort(port)
              .build();

      JFRUploader uploader = buildUploader();
      JFRJMXRecorder connector = JFRJMXRecorder.connect(config);
      var processor = new JFRController(uploader, connector);
      processor.setup();
      processor.loop(config.getHarvestInterval());
    } catch (Throwable e) {
      logger.error("JFR Controller is crashing!", e);
      throw new RuntimeException(e);
    }
  }

  static JFRUploader buildUploader() throws UnknownHostException, MalformedURLException {
    String localIpAddr = InetAddress.getLocalHost().toString();
    var attr =
        COMMON_ATTRIBUTES
            .put(APP_NAME, appName())
            .put(SERVICE_NAME, appName())
            .put(HOSTNAME, localIpAddr);

    var fileToBatches =
        FileToBufferedTelemetry.builder()
            .commonAttributes(attr)
            .metricMappers(ToMetricRegistry.createDefault())
            .eventMapper(ToEventRegistry.createDefault())
            .summaryMappers(ToSummaryRegistry.createDefault())
            .build();
    TelemetryClient telemetryClient = new TelemetryClientFactory().build();
    return new JFRUploader(telemetryClient, fileToBatches);
  }

  private static String appName() {
    // FIXME Read this from an appropriate event in the file
    String appName = System.getenv(ENV_APP_NAME);
    return appName == null ? "eventing_hobgoblin" : appName;
  }
}
