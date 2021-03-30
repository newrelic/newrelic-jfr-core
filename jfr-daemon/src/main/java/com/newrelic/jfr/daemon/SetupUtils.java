package com.newrelic.jfr.daemon;

import static java.util.function.Function.identity;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.EventBatchSenderFactory;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.SenderConfiguration.SenderConfigurationBuilder;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import java.net.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import jdk.jfr.consumer.RecordedEvent;

public class SetupUtils {

  private SetupUtils() {}

  /**
   * Build a base set of common attributes.
   *
   * @return the attributes
   */
  public static Attributes buildCommonAttributes() {
    Attributes attributes =
        new com.newrelic.telemetry.Attributes()
            .put(AttributeNames.INSTRUMENTATION_NAME, "JFR")
            .put(AttributeNames.INSTRUMENTATION_PROVIDER, "JFR-Uploader")
            .put(AttributeNames.COLLECTOR_NAME, "JFR-Uploader");
    String hostname;
    try {
      hostname = InetAddress.getLocalHost().toString();
    } catch (Throwable e) {
      hostname = InetAddress.getLoopbackAddress().toString();
    }
    attributes.put(AttributeNames.HOSTNAME, hostname);
    return attributes;
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
    BlockingQueue<RecordedEvent> queue = new LinkedBlockingQueue<>(250_000);
    RecordedEventBuffer recordedEventBuffer = new RecordedEventBuffer(queue);
    return new JFRUploader(telemetryClient, recordedEventBuffer);
  }

  private static TelemetryClient buildTelemetryClient(DaemonConfig config) {
    System.out.println(config.toString());
    Supplier<HttpPoster> httpPosterCreator =
        () -> new OkHttpPoster(Duration.of(10, ChronoUnit.SECONDS));
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
    return "JFR-Daemon/" + config.getDaemonVersion();
  }
}
