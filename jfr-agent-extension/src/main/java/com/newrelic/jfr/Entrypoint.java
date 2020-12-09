package com.newrelic.jfr;

import static com.newrelic.jfr.daemon.JFRDaemon.*;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.jfr.agent.AgentController;
import com.newrelic.jfr.daemon.*;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.management.JMException;
import org.slf4j.LoggerFactory;

public class Entrypoint {
  public static final URI DEFAULT_METRIC_INGEST_URI = URI.create("https://metric-api.newrelic.com");
  public static final URI DEFAULT_EVENT_INGEST_URI =
      URI.create("https://insights-collector.newrelic.com/v1/accounts/events");
  public static final String METRIC_INGEST_URI = "metric_ingest_uri";
  public static final String INSERT_API_KEY = "insert_api_key";
  public static final String JFR_ENABLED = "jfr.enabled";
  public static final String JFR_AUDIT_MODE = "jfr.audit_mode";

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
    Logger logger = agent.getLogger();
    var agentConfig = agent.getConfig();

    if (isJfrDisabled(agentConfig)) {
      logger.log(
          Level.INFO,
          "New Relic JFR Monitor is disabled: JFR config has not been enabled in the Java agent.");
      return;
    }

    logger.log(Level.INFO, "Attaching New Relic JFR Monitor");

    try {
      new Entrypoint().start();
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Unable to attach JFR Monitor", t);
    }
  }

  private void start() throws IOException, JMException {

    var config = buildConfig();
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
          } catch (IOException | JMException e) {
            logger.error("Error in agent, shutting down", e);
          }
        });
  }

  static boolean isJfrDisabled(com.newrelic.api.agent.Config agentConfig) {
    return !agentConfig.getValue(JFR_ENABLED, false);
  }
}
