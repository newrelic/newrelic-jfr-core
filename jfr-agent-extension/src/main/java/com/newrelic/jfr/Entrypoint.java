package com.newrelic.jfr;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.jfr.daemon.JfrController;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.newrelic.jfr.daemon.SetupUtils;
import org.slf4j.LoggerFactory;

import static com.newrelic.jfr.daemon.SetupUtils.buildConfig;
import static com.newrelic.jfr.daemon.SetupUtils.buildUploader;

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
    var agentConfig = agent.getConfig();

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

  private void start() throws IOException {

    var config = buildConfig();
    var attr = SetupUtils.buildCommonAttributes();
    var eventConverter = buildEventConverter(attr);
    var eventConverterReference = new AtomicReference<>(eventConverter);
    var readinessCheck = new AtomicBoolean(true);
    var uploader = buildUploader(config); // , readinessCheck, eventConverterReference
    var jfrController = new JfrController(uploader, config);
    var jfrMonitorService = Executors.newSingleThreadExecutor();
    jfrMonitorService.submit(
        () -> {
          try {
            jfrController.loop();
          } catch (IOException e) {
            logger.info("Error in agent, shutting down", e);
          }
        });
  }

  static boolean isJfrDisabled(com.newrelic.api.agent.Config agentConfig) {
    return !agentConfig.getValue(JFR_ENABLED, false);
  }
}
