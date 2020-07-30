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
import com.newrelic.jfr.daemon.lifecycle.MBeanServerConnector;
import com.newrelic.jfr.daemon.lifecycle.RemoteEntityGuid;
import com.newrelic.telemetry.TelemetryClient;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import javax.management.MBeanServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRDaemon {
  private static final Logger logger = LoggerFactory.getLogger(JFRDaemon.class);

  public static void main(String[] args) {
    try {
      DaemonConfig config = buildConfig();
      MBeanServerConnection mBeanServerConnection =
          new MBeanServerConnector(config).getConnection();
      String entityGuid = new RemoteEntityGuid(mBeanServerConnection).queryFromJmx();
      var uploader = buildUploader(config, entityGuid);
      var jfrController = new JFRController(uploader, config);
      jfrController.setup();
      jfrController.loop(config.getHarvestInterval());
    } catch (Throwable e) {
      logger.error("JFR Daemon is crashing!", e);
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

  static JFRUploader buildUploader(DaemonConfig config, String entityGuid)
      throws MalformedURLException {
    String hostname = findHostname();
    var attr =
        COMMON_ATTRIBUTES
            .put(APP_NAME, config.getMonitoredAppName())
            .put(SERVICE_NAME, config.getMonitoredAppName())
            .put(HOSTNAME, hostname);
    if (entityGuid != null) {
      attr.put("entity.guid", entityGuid);
    }

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
