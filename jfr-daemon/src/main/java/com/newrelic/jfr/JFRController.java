package com.newrelic.jfr;

import static com.newrelic.jfr.AttributeNames.APP_NAME;
import static com.newrelic.jfr.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.AttributeNames.SERVICE_NAME;
import static com.newrelic.jfr.EnvironmentVars.ENV_APP_NAME;
import static com.newrelic.jfr.JFRUploader.COMMON_ATTRIBUTES;

import com.newrelic.telemetry.TelemetryClient;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private static final int DELAY_BETWEEN_DUMPS_MS = 10_000;

  private final JFRJMXConnector connector;
  private final JFRUploader uploader;
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          2,
          r -> {
            Thread result = new Thread("JFRController");
            result.setDaemon(true);
            return result;
          });

  private final boolean streamFromJmx = true;
  private volatile boolean shutdown = false;

  public JFRController(JFRUploader uploader, JFRJMXConnector connector) {
    this.connector = connector;
    this.uploader = uploader;
  }

  // This needs to be exposed to JMX / k8s
  public void shutdown() {
    shutdown = true;
  }

  void setup() {
    try {
      connector.makeConnection();
      connector.startRecording();
    } catch (Exception e) {
      e.printStackTrace();
      shutdown();
      throw new RuntimeException(e);
    }
  }

  void loop() throws IOException {
    while (!shutdown) {
      try {
        Thread.sleep(DELAY_BETWEEN_DUMPS_MS);
      } catch (InterruptedException e) {
        // Ignore the premature return and trigger the next JMX dump at once
      }
      try {
        final var pathToFile =
            streamFromJmx ? connector.streamRecordingToFile() : connector.copyRecordingToFile();

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
    try {
      JFRUploader uploader = buildUploader();
      JFRJMXConnector connector = new JFRJMXConnector(DEFAULT_PORT);
      var processor = new JFRController(uploader, connector);
      processor.setup();
      processor.loop();
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