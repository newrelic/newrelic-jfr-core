/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static java.util.function.Function.identity;

import com.newrelic.jfr.daemon.agent.FileJfrRecorderFactory;
import com.newrelic.jfr.daemon.httpclient.ApacheHttpPoster;
import com.newrelic.jfr.daemon.httpclient.ApacheProxyManager;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.EventBatchSenderFactory;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.SenderConfiguration.SenderConfigurationBuilder;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupUtils {
  private static final Logger logger = LoggerFactory.getLogger(SetupUtils.class);
  public static final String JFR_DAEMON = "JFR-Daemon/";
  public static final int DEFAULT_HTTP_POSTER_TIMEOUT_MILLIS = 120_000;

  private SetupUtils() {}

  /**
   * Build a base set of common attributes.
   *
   * @param config the daemon config
   * @return the attributes
   */
  public static Attributes buildCommonAttributes(DaemonConfig config) {
    Attributes attributes =
        new com.newrelic.telemetry.Attributes()
            .put(AttributeNames.INSTRUMENTATION_NAME, "JFR")
            .put(AttributeNames.INSTRUMENTATION_PROVIDER, "JFR-Uploader")
            .put(AttributeNames.COLLECTOR_NAME, "JFR-Uploader");
    attributes.put(AttributeNames.APP_NAME, config.getMonitoredAppName());
    attributes.put(AttributeNames.SERVICE_NAME, config.getMonitoredAppName());

    // Add service.instance.id from environment if available
    String serviceInstanceId = getServiceInstanceIdFromEnvironment();
    if (serviceInstanceId != null) {
      attributes.put("service.instance.id", serviceInstanceId);
    }

    return attributes;
  }

  /**
   * Get service.instance.id from environment variables. First tries OTEL_RESOURCE_ATTRIBUTES, then
   * falls back to SERVICE_INSTANCE_ID.
   */
  private static String getServiceInstanceIdFromEnvironment() {
    // First try to get it from OTEL_RESOURCE_ATTRIBUTES
    String otelResourceAttributes = System.getenv("OTEL_RESOURCE_ATTRIBUTES");
    if (otelResourceAttributes != null) {
      String serviceInstanceId = extractServiceInstanceId(otelResourceAttributes);
      if (serviceInstanceId != null) {
        return serviceInstanceId;
      }
    }

    // Fallback to SERVICE_INSTANCE_ID environment variable
    return System.getenv(EnvironmentVars.SERVICE_INSTANCE_ID);
  }

  /**
   * Extract service.instance.id from OTEL_RESOURCE_ATTRIBUTES string. Example:
   * "service.name=app,service.instance.id=instance-1" returns "instance-1"
   */
  private static String extractServiceInstanceId(String otelResourceAttributes) {
    if (otelResourceAttributes == null || otelResourceAttributes.isEmpty()) {
      return null;
    }

    String[] pairs = otelResourceAttributes.split(",");
    for (String pair : pairs) {
      String[] keyValue = pair.split("=", 2);
      if (keyValue.length == 2 && "service.instance.id".equals(keyValue[0].trim())) {
        return keyValue[1].trim();
      }
    }
    return null;
  }

  /**
   * Parse the environment and build a config instance.
   *
   * @return the config
   */
  public static DaemonConfig buildConfig() {
    String daemonVersion = VersionFinder.getVersion();

    DaemonConfig.Builder builder =
        DaemonConfig.builder()
            .apiKey(System.getenv(EnvironmentVars.INSERT_API_KEY))
            .daemonVersion(daemonVersion);

    builder.maybeEnv(EnvironmentVars.ENV_APP_NAME, identity(), builder::monitoredAppName);
    builder.maybeEnv(EnvironmentVars.REMOTE_JMX_HOST, identity(), builder::jmxHost);
    builder.maybeEnv(EnvironmentVars.REMOTE_JMX_PORT, Integer::parseInt, builder::jmxPort);
    builder.maybeEnv(EnvironmentVars.METRICS_INGEST_URI, URI::create, builder::metricsUri);
    builder.maybeEnv(EnvironmentVars.EVENTS_INGEST_URI, URI::create, builder::eventsUri);
    builder.maybeEnv(
        EnvironmentVars.JFR_SHARED_FILESYSTEM, Boolean::parseBoolean, builder::useSharedFilesystem);
    builder.maybeEnv(
        EnvironmentVars.USE_LICENSE_KEY, Boolean::parseBoolean, builder::useLicenseKey);
    builder.maybeEnv(EnvironmentVars.AUDIT_LOGGING, Boolean::parseBoolean, builder::auditLogging);
    builder.maybeEnv(EnvironmentVars.PROXY_HOST, identity(), builder::proxyHost);
    builder.maybeEnv(EnvironmentVars.PROXY_PORT, Integer::parseInt, builder::proxyPort);
    builder.maybeEnv(EnvironmentVars.PROXY_USER, identity(), builder::proxyUser);
    builder.maybeEnv(EnvironmentVars.PROXY_PASSWORD, identity(), builder::proxyPassword);
    builder.maybeEnv(EnvironmentVars.PROXY_SCHEME, identity(), builder::proxyScheme);
    builder.maybeEnv(EnvironmentVars.THREAD_NAME_PATTERN, identity(), builder::threadNamePattern);
    builder.maybeEnv(EnvironmentVars.HARVEST_INTERVAL, Integer::parseInt, builder::harvestInterval);
    builder.maybeEnv(EnvironmentVars.QUEUE_SIZE, Integer::parseInt, builder::queueSize);

    return builder.build();
  }

  public static DaemonConfig buildDynamicAttachConfig(String agentArgs) {
    String daemonVersion = VersionFinder.getVersion();
    DaemonConfig.Builder builder =
        DaemonConfig.builder()
            .useLicenseKey(true) // dynamic attach only works with license key
            .daemonVersion(daemonVersion);

    // key | app_name | metrics_uri | events_uri
    String[] args = agentArgs.split("|");
    String apiKey;
    if (args.length == 4) {
      try {
        builder.apiKey(args[0]);
        builder.monitoredAppName(args[1]);
        builder.metricsUri(new URI(args[2]));
        builder.eventsUri(new URI(args[3]));
      } catch (URISyntaxException urix) {
        throw new RuntimeException("Bad URI in config", urix);
      }
    }
    if (args.length == 2) {
      builder.apiKey(args[0]);
      builder.monitoredAppName(args[1]);
    } else {
      throw new RuntimeException("Wrong number of arguments to config: " + agentArgs);
    }

    return builder.build();
  }

  /**
   * Build a {@link JFRUploader} with the {@code config}.
   *
   * @param config the config
   * @return the uploader
   */
  public static JFRUploader buildUploader(DaemonConfig config) {
    TelemetryClient telemetryClient = buildTelemetryClient(config);
    BlockingQueue<RecordedEvent> queue = new LinkedBlockingQueue<>(config.getQueueSize());
    RecordedEventBuffer recordedEventBuffer = new RecordedEventBuffer(queue);
    return new JFRUploader(new NewRelicTelemetrySender(telemetryClient), recordedEventBuffer);
  }

  /**
   * Build a {@link JfrController} with the {@code config} and {@code uploader}.
   *
   * <p>This method is called by the New Relic Java Agent.
   *
   * @param config the config
   * @param uploader the uploader
   * @return the JfrController
   */
  public static JfrController buildJfrController(DaemonConfig config, JFRUploader uploader) {
    FileJfrRecorderFactory recorderFactory =
        new FileJfrRecorderFactory(config.getHarvestInterval());
    return new JfrController(recorderFactory, uploader, config.getHarvestInterval());
  }

  private static TelemetryClient buildTelemetryClient(DaemonConfig config) {
    Supplier<HttpPoster> httpPosterCreator =
        () ->
            new ApacheHttpPoster(
                new ApacheProxyManager(config), null, DEFAULT_HTTP_POSTER_TIMEOUT_MILLIS);

    MetricBatchSender metricBatchSender = buildMetricBatchSender(config, httpPosterCreator);
    EventBatchSender eventBatchSender = buildEventBatchSender(config, httpPosterCreator);
    return new TelemetryClient(metricBatchSender, null, eventBatchSender, null);
  }

  private static EventBatchSender buildEventBatchSender(
      DaemonConfig config, Supplier<HttpPoster> httpPosterCreator) {
    SenderConfigurationBuilder eventsConfig =
        EventBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
            .configureWith(config.getApiKey())
            .auditLoggingEnabled(config.auditLogging())
            .secondaryUserAgent(makeUserAgent(config))
            .useLicenseKey(config.useLicenseKey());
    if (config.getEventsUri() != null) {
      eventsConfig = eventsConfig.endpoint(toURL(config.getEventsUri()));
    }
    if (config.useLicenseKey()) {
      eventsConfig = eventsConfig.useLicenseKey(true);
    }
    return EventBatchSender.create(eventsConfig.build());
  }

  private static MetricBatchSender buildMetricBatchSender(
      DaemonConfig config, Supplier<HttpPoster> httpPosterCreator) {
    SenderConfigurationBuilder metricConfig =
        MetricBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
            .configureWith(config.getApiKey())
            .secondaryUserAgent(makeUserAgent(config))
            .auditLoggingEnabled(config.auditLogging())
            .useLicenseKey(config.useLicenseKey());
    if (config.getMetricsUri() != null) {
      metricConfig = metricConfig.endpoint(toURL(config.getMetricsUri()));
    }
    if (config.useLicenseKey()) {
      metricConfig = metricConfig.useLicenseKey(true);
    }
    return MetricBatchSender.create(metricConfig.build());
  }

  private static URL toURL(URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Could not convert URI to URL.", e);
    }
  }

  private static String makeUserAgent(DaemonConfig config) {
    return JFR_DAEMON + config.getDaemonVersion();
  }
}
