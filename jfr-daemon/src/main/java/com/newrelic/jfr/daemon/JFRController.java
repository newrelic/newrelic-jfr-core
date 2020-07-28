package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;
import static com.newrelic.jfr.daemon.EnvironmentVars.ENV_APP_NAME;
import static com.newrelic.jfr.daemon.EnvironmentVars.EVENTS_INGEST_URI;
import static com.newrelic.jfr.daemon.EnvironmentVars.INSERT_API_KEY;
import static com.newrelic.jfr.daemon.EnvironmentVars.JFR_SHARED_FILESYSTEM;
import static com.newrelic.jfr.daemon.EnvironmentVars.METRICS_INGEST_URI;
import static com.newrelic.jfr.daemon.EnvironmentVars.REMOTE_JMX_HOST;
import static com.newrelic.jfr.daemon.EnvironmentVars.REMOTE_JMX_PORT;
import static com.newrelic.jfr.daemon.JFRUploader.COMMON_ATTRIBUTES;
import static java.util.function.Function.identity;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.telemetry.TelemetryClient;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
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

  // Non-final to allow for reconnect - there's too much crufty JMX state too close to the surface
  private JFRJMXRecorder recorder;
  private final JFRUploader uploader;
  private final DaemonConfig config;

  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          2,
          r -> {
            Thread result = new Thread(r, "JFRController");
            result.setDaemon(true);
            return result;
          });

  private volatile boolean shutdown = false;

  public JFRController(JFRUploader uploader, JFRJMXRecorder recorder, DaemonConfig config) {
    this.recorder = recorder;
    this.uploader = uploader;
    this.config = config;
  }

  // This needs to be exposed to JMX / k8s
  public void shutdown() {
    shutdown = true;
  }

  void setup() {
    try {
      recorder.startRecordingWithBackOff();
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
        final var pathToFile = recorder.recordToFile();
        executorService.submit(() -> uploader.handleFile(pathToFile));
      } catch (MalformedObjectNameException
          | MBeanException
          | InstanceNotFoundException
          | OpenDataException
          | ReflectionException e) {
        logger.error("JMX streaming failed: ", e);
        try {
          recorder = JFRJMXRecorder.connectWithBackOff(config);
          recorder.startRecordingWithBackOff();
        } catch (MalformedObjectNameException
            | MBeanException
            | InstanceNotFoundException
            | OpenDataException
            | ReflectionException jmxException) {
          // Log before fatal exit?
          shutdown();
        }
      }
    }
    executorService.shutdown();
  }

  public static void main(String[] args) {

    DaemonConfig config = buildConfig();

    try {
      var uploader = buildUploader(config);
      var recorder = JFRJMXRecorder.connectWithBackOff(config);
      var processor = new JFRController(uploader, recorder, config);
      processor.setup();
      processor.loop(config.getHarvestInterval());
    } catch (Throwable e) {
      logger.error("JFR Controller is crashing!", e);
      throw new RuntimeException(e);
    }
  }

  private static DaemonConfig buildConfig() {

    var daemonVersion = new VersionFinder().get();

    var builder =
        DaemonConfig.builder().apiKey(System.getenv(INSERT_API_KEY)).daemonVersion(daemonVersion);

    builder.maybeEnv(ENV_APP_NAME, identity(), builder::monitoredAppName);
    builder.maybeEnv(REMOTE_JMX_HOST, identity(), builder::jmxHost);
    builder.maybeEnv(REMOTE_JMX_PORT, Integer::parseInt, builder::jmxPort);
    builder.maybeEnv(METRICS_INGEST_URI, URI::create, builder::metricsUri);
    builder.maybeEnv(EVENTS_INGEST_URI, URI::create, builder::eventsUri);
    builder.maybeEnv(JFR_SHARED_FILESYSTEM, Boolean::parseBoolean, builder::useSharedFilesystem);

    return builder.build();
  }

  static JFRUploader buildUploader(DaemonConfig config)
      throws UnknownHostException, MalformedURLException {
    String localIpAddr = InetAddress.getLocalHost().toString();
    var attr =
        COMMON_ATTRIBUTES
            .put(APP_NAME, config.getMonitoredAppName())
            .put(SERVICE_NAME, config.getMonitoredAppName())
            .put(HOSTNAME, localIpAddr);

    var fileToBatches =
        FileToBufferedTelemetry.builder()
            .commonAttributes(attr)
            .metricMappers(ToMetricRegistry.createDefault())
            .eventMapper(ToEventRegistry.createDefault())
            .summaryMappers(ToSummaryRegistry.createDefault())
            .build();
    TelemetryClient telemetryClient = new TelemetryClientFactory().build(config);
    return new JFRUploader(telemetryClient, fileToBatches);
  }
}
