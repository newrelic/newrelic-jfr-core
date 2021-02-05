package com.newrelic.jfr;

import static com.newrelic.jfr.daemon.SetupUtils.buildConfig;
import static com.newrelic.jfr.daemon.SetupUtils.buildUploader;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.jfr.daemon.*;
import com.newrelic.telemetry.Attributes;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.LoggerFactory;

public class Entrypoint {

  public static final String JFR_ENABLED = "jfr.enabled";

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Entrypoint.class);

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
      new Entrypoint().start();
    } catch (Throwable t) {
      logger.error("Unable to attach JFR Monitor", t);
    }
  }

  private void start() {
    DaemonConfig config = buildConfig();
    Attributes attr = SetupUtils.buildCommonAttributes();
    //    var eventConverterReference = new AtomicReference<>(eventConverter);
    //    var readinessCheck = new AtomicBoolean(true);
    JFRUploader uploader = buildUploader(config);
    uploader.readyToSend(new EventConverter(attr));

    JfrRecorderFactory factory = null;
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
}
