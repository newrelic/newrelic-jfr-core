package com.newrelic.jfr;

import static com.newrelic.jfr.daemon.SetupUtils.buildConfig;
import static com.newrelic.jfr.daemon.SetupUtils.buildUploader;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.jfr.daemon.*;
import com.newrelic.jfr.daemon.agent.FileJfrRecorderFactory;
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

    Config agentConfig = NewRelic.getAgent().getConfig();

    if (isJfrDisabled(agentConfig)) {
      logger.info(
          "New Relic JFR Monitor is disabled: JFR config has not been enabled in the Java agent.");
      return;
    }

    logger.info("Attaching New Relic JFR Monitor");

    try {
      new Entrypoint().start(agentConfig);
    } catch (Throwable t) {
      logger.error("Unable to attach JFR Monitor", t);
    }
  }

  private void start(Config agentConfig) {
    DaemonConfig config = buildConfig();
    Attributes attr = SetupUtils.buildCommonAttributes();
    JFRUploader uploader = buildUploader(config);
    String threadNamePattern =
        agentConfig.getValue("thread_sampler.name_pattern", ThreadNameNormalizer.DEFAULT_PATTERN);
    uploader.readyToSend(new EventConverter(attr, threadNamePattern));
    FileJfrRecorderFactory recorderFactory =
        new FileJfrRecorderFactory(config.getHarvestInterval());
    JfrController jfrController =
        new JfrController(recorderFactory, uploader, config.getHarvestInterval());

    ExecutorService jfrMonitorService = Executors.newSingleThreadExecutor();
    jfrMonitorService.submit(
        () -> {
          try {
            jfrController.loop();
          } catch (JfrRecorderException e) {
            logger.info("Error in agent, shutting down", e);
            jfrController.shutdown();
          }
        });
  }

  static boolean isJfrDisabled(com.newrelic.api.agent.Config agentConfig) {
    return !agentConfig.getValue(JFR_ENABLED, false);
  }
}
