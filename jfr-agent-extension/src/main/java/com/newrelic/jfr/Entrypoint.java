package com.newrelic.jfr;

import static com.newrelic.jfr.daemon.SetupUtils.buildUploader;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.jfr.daemon.*;
import com.newrelic.jfr.daemon.agent.FileJfrRecorderFactory;
import com.newrelic.telemetry.Attributes;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.LoggerFactory;

public class Entrypoint {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Entrypoint.class);

  private static final String APP_NAME = "app_name";
  private static final String LICENSE_KEY = "license_key";
  private static final String JFR_ENABLED = "jfr.enabled";
  private static final String METRICS_URI = "jfr.metrics_uri";
  private static final String EVENTS_URI = "jfr.events_uri";

  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      Class.forName("jdk.jfr.Recording");
      Class.forName("jdk.jfr.FlightRecorder");
    } catch (ClassNotFoundException __) {
      logger.error("Core JFR APIs do not exist in this JVM, can't attach");
      return;
    }

    Agent agent = NewRelic.getAgent();
    Config agentConfig = agent.getConfig();

    if (isJfrDisabled(agentConfig)) {
      logger.info(
          "New Relic JFR Monitor is disabled: JFR config has not been enabled in the Java agent.");
      return;
    }

    logger.info("Attaching New Relic JFR Monitor");
    try {
      DaemonConfig config = buildConfig(agentConfig);
      new Entrypoint().start(config);
    } catch (Throwable t) {
      logger.error("Unable to attach JFR Monitor", t);
    }
  }

  private void start(DaemonConfig config) {
    Attributes attr = SetupUtils.buildCommonAttributes();
    //    var eventConverterReference = new AtomicReference<>(eventConverter);
    //    var readinessCheck = new AtomicBoolean(true);
    JFRUploader uploader = buildUploader(config);
    uploader.readyToSend(new EventConverter(attr));

    JfrRecorderFactory factory = new FileJfrRecorderFactory(Duration.ofSeconds(10));
    JfrController jfrController = new JfrController(factory, uploader, config.getHarvestInterval());
    ExecutorService jfrMonitorService = Executors.newSingleThreadExecutor();
    jfrMonitorService.submit(
        () -> {
          try {
            jfrController.loop();
          } catch (JfrRecorderException e) {
            logger.info("Error in agent, shutting down", e);
          }
        });
  }

  static boolean isJfrDisabled(com.newrelic.api.agent.Config agentConfig) {
    return !agentConfig.getValue(JFR_ENABLED, false);
  }

  /**
   * Parse the agent config and build a config instance.
   *
   * @return the config
   */
  static DaemonConfig buildConfig(Config agentConfig) throws URISyntaxException {
    String daemonVersion = VersionFinder.getVersion();

    DaemonConfig.Builder builder = null;
    builder =
        DaemonConfig.builder()
            .apiKey(agentConfig.getValue(LICENSE_KEY))
            .monitoredAppName(agentConfig.getValue(APP_NAME))
            .useLicenseKey(true)
            .metricsUri(new URI(agentConfig.getValue(METRICS_URI)))
            .eventsUri(new URI(agentConfig.getValue(EVENTS_URI)))
            .daemonVersion(daemonVersion);

    //    builder.maybeEnv(EnvironmentVars.AUDIT_LOGGING, Boolean::parseBoolean,
    // builder::auditLogging);

    return builder.build();
  }
}
