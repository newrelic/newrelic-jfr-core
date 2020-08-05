package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;
import static com.newrelic.jfr.daemon.DumpFileProcessor.COMMON_ATTRIBUTES;
import static com.newrelic.jfr.daemon.EnvironmentVars.ENV_APP_NAME;
import static com.newrelic.jfr.daemon.EnvironmentVars.EVENTS_INGEST_URI;
import static com.newrelic.jfr.daemon.EnvironmentVars.INSERT_API_KEY;
import static com.newrelic.jfr.daemon.EnvironmentVars.JFR_SHARED_FILESYSTEM;
import static com.newrelic.jfr.daemon.EnvironmentVars.METRICS_INGEST_URI;
import static com.newrelic.jfr.daemon.EnvironmentVars.REMOTE_JMX_HOST;
import static com.newrelic.jfr.daemon.EnvironmentVars.REMOTE_JMX_PORT;
import static java.util.function.Function.identity;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.daemon.buffer.BufferedTelemetry;
import com.newrelic.jfr.daemon.lifecycle.MBeanServerConnector;
import com.newrelic.jfr.daemon.lifecycle.RemoteEntityGuid;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryClient;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRDaemon {
  private static final Logger logger = LoggerFactory.getLogger(JFRDaemon.class);

  public static void main(String[] args) {
    try {
      var config = buildConfig();
      var mBeanServerConnection = new MBeanServerConnector(config).getConnection();

      // TODO: Reevaluate capacity
      var rawEventQueue = new LinkedBlockingQueue<RecordedEvent>(100_000);
      var transformBufferAndSendExecutor = Executors.newSingleThreadExecutor();

      transformBufferAndSendExecutor.submit(
          () -> {
            RawEventConsumerTask rawEventSink =
                buildRawEventConsumerTask(config, mBeanServerConnection, rawEventQueue);
            rawEventSink.run();
          });

      JFRController jfrController = buildJfrController(config, rawEventQueue);
      jfrController.setup();
      jfrController.runUntilShutdown();
    } catch (Throwable e) {
      logger.error("JFR Daemon is crashing!", e);
      throw new RuntimeException(e);
    }
  }

  private static RawEventConsumerTask buildRawEventConsumerTask(
      DaemonConfig config,
      javax.management.MBeanServerConnection mBeanServerConnection,
      LinkedBlockingQueue<RecordedEvent> rawEventQueue) {
    var toSummaryRegistry = ToSummaryRegistry.createDefault();
    var entityGuid = new RemoteEntityGuid(mBeanServerConnection).queryFromJmx();
    var eventToTelemetry = buildRecordedEventToTelemetry(toSummaryRegistry);
    var commonAttributes = buildCommonAttributes(config, entityGuid);
    var bufferedTelemetry = BufferedTelemetry.create(commonAttributes);
    var lastSendStateTracker = buildLastSendStateTracker(config);
    var uploader = buildJfrUploader(config);
    return RawEventConsumerTask.builder()
        .rawEventQueue(rawEventQueue)
        .bufferedTelemetry(bufferedTelemetry)
        .recordedEventToTelemetry(eventToTelemetry)
        .lastSendStateTracker(lastSendStateTracker)
        .toSummaryRegistry(toSummaryRegistry)
        .uploader(uploader)
        .build();
  }

  private static JFRUploader buildJfrUploader(DaemonConfig config) {
    try {
      TelemetryClient telemetryClient = new TelemetryClientFactory().build(config);
      return new JFRUploader(telemetryClient);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Cannot create TelemetryClient, daemon crashing", e);
    }
  }

  private static LastSendStateTracker buildLastSendStateTracker(DaemonConfig config) {
    return new LastSendStateTracker(
        config.getBatchSendInterval(), config.getMaxBatchSizeBeforeSend());
  }

  private static JFRController buildJfrController(
      DaemonConfig config, LinkedBlockingQueue<RecordedEvent> rawEventQueue) {
    var fileToRawEventSource = buildFileToRawEventSource(rawEventQueue);
    var dumpFileProcessor = buildDumpFileProcessor(fileToRawEventSource);
    var periodicRecordingService = buildPeriodicRecordingExecutorService();
    return new JFRController(dumpFileProcessor, config, periodicRecordingService);
  }

  private static FileToRawEventSource buildFileToRawEventSource(
      LinkedBlockingQueue<RecordedEvent> rawEventQueue) {
    return new FileToRawEventSource(
        event -> {
          try {
            rawEventQueue.put(event);
          } catch (InterruptedException e) {
            logger.warn("Interrupted while sending raw event", e);
            Thread.currentThread().interrupt();
          }
        });
  }

  private static ScheduledExecutorService buildPeriodicRecordingExecutorService() {
    return Executors.newSingleThreadScheduledExecutor(
        r -> {
          Thread result = new Thread(r, "JFRController");
          result.setDaemon(true);
          return result;
        });
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

  static DumpFileProcessor buildDumpFileProcessor(FileToRawEventSource fileToRawEventSource) {
    return new DumpFileProcessor(fileToRawEventSource);
  }

  static RecordedEventToTelemetry buildRecordedEventToTelemetry(
      ToSummaryRegistry toSummaryRegistry) {
    return RecordedEventToTelemetry.builder()
        .metricMappers(ToMetricRegistry.createDefault())
        .eventMappers(ToEventRegistry.createDefault())
        .summaryMappers(toSummaryRegistry)
        .build();
  }

  static Attributes buildCommonAttributes(DaemonConfig config, Optional<String> entityGuid) {
    String hostname = findHostname();
    var attr =
        COMMON_ATTRIBUTES.put(SERVICE_NAME, config.getMonitoredAppName()).put(HOSTNAME, hostname);
    entityGuid.ifPresentOrElse(
        guid -> attr.put("entity.guid", guid),
        () -> attr.put(APP_NAME, config.getMonitoredAppName()));
    return attr;
  }

  private static String findHostname() {
    try {
      return InetAddress.getLocalHost().toString();
    } catch (Throwable e) {
      var loopback = InetAddress.getLoopbackAddress().toString();
      logger.error(
          "Unable to get localhost IP, defaulting to loopback address," + loopback + ".", e);
      return loopback;
    }
  }
}
