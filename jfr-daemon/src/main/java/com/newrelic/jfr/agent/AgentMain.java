package com.newrelic.jfr.agent;

import static com.newrelic.jfr.daemon.JFRDaemon.*;

import com.newrelic.jfr.daemon.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentMain {
  private static final Logger logger = LoggerFactory.getLogger(AgentMain.class);

  public static void agentmain(String agentArgs, Instrumentation inst) {
    DaemonConfig config = buildConfigFromArgs(agentArgs);
    actualmain(config);
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    actualmain(buildConfig());
  }

  public static void actualmain(DaemonConfig config) {
    try {
      Class.forName("jdk.jfr.Recording");
      Class.forName("jdk.jfr.FlightRecorder");
    } catch (ClassNotFoundException __) {
      logger.error("Core JFR APIs do not exist in this JVM, can't attach");
      return;
    }

    try {
      logger.info("Attaching JFR Monitor");
      new AgentMain().start(config);
    } catch (Throwable t) {
      logger.error("Unable to attach JFR Monitor", t);
    }
  }

  private void start(DaemonConfig config) throws IOException {
    var attr = new JFRCommonAttributes(config).build(Optional.empty());
    var eventConverter = buildEventConverter(attr);
    var eventConverterReference = new AtomicReference<>(eventConverter);
    var readinessCheck = new AtomicBoolean(true);
    var uploader = buildUploader(config, readinessCheck, eventConverterReference);
    var jfrController = new AgentController(uploader, config);
    jfrController.startRecording();
    var jfrMonitorService = Executors.newSingleThreadExecutor();
    jfrMonitorService.submit(
        () -> {
          try {
            jfrController.loop(config.getHarvestInterval());
          } catch (IOException e) {
            logger.error("Error in agent, shutting down", e);
            jfrController.cleanup();
          }
        });
  }
}
