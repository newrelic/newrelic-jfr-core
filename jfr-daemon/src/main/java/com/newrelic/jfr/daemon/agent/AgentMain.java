package com.newrelic.jfr.daemon.agent;

import com.newrelic.jfr.daemon.EventConverter;
import com.newrelic.jfr.daemon.JfrController;
import com.newrelic.jfr.daemon.SetupUtils;
import java.lang.instrument.Instrumentation;
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
    var config = SetupUtils.buildConfig();
    var commonAttrs = SetupUtils.buildCommonAttributes();
    var uploader = SetupUtils.buildUploader(config);
    uploader.readyToSend(new EventConverter(commonAttrs));
    var recorderFactory = new FileJfrRecorderFactory(config.getHarvestInterval());
    var controller = new JfrController(recorderFactory, uploader, config.getHarvestInterval());

    var executorService = Executors.newSingleThreadExecutor();
    executorService.submit(
        () -> {
          try {
            controller.loop();
          } catch (Exception e) {
            logger.error("Error in agent, shutting down.", e);
          }
        });
  }
}
