package com.newrelic.jfr;

import static com.newrelic.jfr.daemon.AttributeNames.ENTITY_GUID;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;
import static com.newrelic.jfr.daemon.SetupUtils.buildUploader;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.jfr.daemon.*;
import com.newrelic.jfr.daemon.agent.FileJfrRecorderFactory;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.Backoff;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
      new Entrypoint().start(config, agent);
    } catch (Throwable t) {
      logger.error("Unable to attach JFR Monitor", t);
    }
  }

  private void start(DaemonConfig config, Agent agent) {
    Attributes commonAttrs = SetupUtils.buildCommonAttributes(config);
    JFRUploader uploader = buildUploader(config);
    String threadNamePattern =
            agent.getConfig().getValue("thread_sampler.name_pattern", ThreadNameNormalizer.DEFAULT_PATTERN);

    fetchRemoteEntityIdAsync(agent)
        .thenAccept(
            optGuid -> {
              if (optGuid.isPresent()) {
                commonAttrs.put(ENTITY_GUID, optGuid.get());
              } else {
                commonAttrs.put(APP_NAME, config.getMonitoredAppName());
                commonAttrs.put(SERVICE_NAME, config.getMonitoredAppName());
              }
              uploader.readyToSend(new EventConverter(commonAttrs, threadNamePattern));
            });

    JfrRecorderFactory factory = new FileJfrRecorderFactory(config.getHarvestInterval());
    JfrController jfrController = new JfrController(factory, uploader, config.getHarvestInterval());

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

  /**
   * Parse the agent config and build a config instance.
   *
   * @return the config
   */
  static DaemonConfig buildConfig(Config agentConfig) throws URISyntaxException {
    String daemonVersion = VersionFinder.getVersion();

    DaemonConfig.Builder builder =
        DaemonConfig.builder()
            .apiKey(agentConfig.getValue(LICENSE_KEY))
            .monitoredAppName(agentConfig.getValue(APP_NAME))
            .useLicenseKey(true)
            .metricsUri(new URI(agentConfig.getValue(METRICS_URI)))
            .eventsUri(new URI(agentConfig.getValue(EVENTS_URI)))
            .daemonVersion(daemonVersion);

    return builder.build();
  }

  CompletableFuture<Optional<String>> fetchRemoteEntityIdAsync(Agent agent) {
    return CompletableFuture.supplyAsync(
        () ->
            awaitRemoteEntityId(
                agent,
                Backoff.builder()
                    .maxBackoff(15, TimeUnit.SECONDS)
                    .backoffFactor(1, TimeUnit.SECONDS)
                    .maxRetries(Integer.MAX_VALUE)
                    .build()));
  }

  Optional<String> awaitRemoteEntityId(Agent agent, Backoff backoff) {
    while (true) {
      long backoffMillis = backoff.nextWaitMs();
      if (backoffMillis == -1) {
        logger.info("Unable to obtain entity guid after backing off. Not setting entity guid.");
        return Optional.empty();
      }

      Map<String, String> linkingMetadata;
      try {
        linkingMetadata = agent.getLinkingMetadata();
      } catch (Exception e) {
        // Exceptions can be thrown when agent initialization is not yet complete
        logger.info(
            "Unable to obtain entity guid because linking metadata is unavailable. Backing off {} millis. ",
            backoffMillis);
        SafeSleep.sleep(Duration.ofMillis(backoffMillis));
        continue;
      }

      Optional<String> optEntityId =
          Optional.ofNullable(linkingMetadata).map(metadata -> metadata.get("entity.guid"));
      if (optEntityId.isPresent()) {
        logger.info("Obtained entity guid: {}", optEntityId.get());
        return optEntityId;
      }
      logger.info(
          "Linking metadata is available but entity guid not yet available. Backing off {} millis.",
          backoffMillis);
      SafeSleep.sleep(Duration.ofMillis(backoffMillis));
    }
  }
}
