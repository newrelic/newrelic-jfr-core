package com.newrelic.jfr.daemon.agent;

import com.newrelic.jfr.daemon.DaemonConfig;
import com.newrelic.jfr.daemon.EventConverter;
import com.newrelic.jfr.daemon.JFRUploader;
import com.newrelic.jfr.daemon.JfrController;
import com.newrelic.jfr.daemon.SetupUtils;
import com.newrelic.telemetry.Attributes;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentMain {
  private static final Logger logger = LoggerFactory.getLogger(AgentMain.class);

  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      Class.forName("jdk.jfr.Recording");
      Class.forName("jdk.jfr.FlightRecorder");
    } catch (ClassNotFoundException __) {
      logger.error("Core JFR APIs do not exist in this JVM, can't attach");
      return;
    }

    try {
      logger.info("Attaching JFR Monitor");
      new AgentMain().start();
    } catch (Throwable t) {
      logger.error("Unable to attach JFR Monitor", t);
    }
  }

  private void start() {
    DaemonConfig config = SetupUtils.buildConfig();
    Attributes commonAttrs = SetupUtils.buildCommonAttributes();
    JFRUploader uploader = SetupUtils.buildUploader(config);
    uploader.readyToSend(new EventConverter(commonAttrs));
    FileJfrRecorderFactory recorderFactory =
        new FileJfrRecorderFactory(config.getHarvestInterval());
    JfrController controller =
        new JfrController(recorderFactory, uploader, config.getHarvestInterval());

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    executorService.submit(
        () -> {
          try {
            controller.loop();
          } catch (Exception e) {
            logger.error("Error in agent, shutting down.", e);
            controller.shutdown();
          }
        });
  }
}
