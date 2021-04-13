package com.newrelic.jfr.daemon;

import static java.util.function.Function.identity;

import com.newrelic.jfr.daemon.agent.FileJfrRecorderFactory;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.EventBatchSenderFactory;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.SenderConfiguration.SenderConfigurationBuilder;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import jdk.jfr.consumer.RecordedEvent;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupUtils {
  private static final Logger logger = LoggerFactory.getLogger(SetupUtils.class);

  private SetupUtils() {}

  /**
   * Build a base set of common attributes.
   *
   * @return the attributes
   * @param config the daemon config
   */
  public static Attributes buildCommonAttributes(DaemonConfig config) {
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
    attributes.put(AttributeNames.APP_NAME, config.getMonitoredAppName());
    attributes.put(AttributeNames.SERVICE_NAME, config.getMonitoredAppName());
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
    builder.maybeEnv(EnvironmentVars.AUDIT_LOGGING, Boolean::parseBoolean, builder::auditLogging);
    builder.maybeEnv(
        EnvironmentVars.USE_LICENSE_KEY, Boolean::parseBoolean, builder::useLicenseKey);
    builder.maybeEnv(EnvironmentVars.PROXY_HOST, identity(), builder::proxyHost);
    builder.maybeEnv(EnvironmentVars.PROXY_PORT, Integer::parseInt, builder::proxyPort);
    builder.maybeEnv(EnvironmentVars.PROXY_USER, identity(), builder::proxyUser);
    builder.maybeEnv(EnvironmentVars.PROXY_PASSWORD, identity(), builder::proxyPassword);
    builder.maybeEnv(EnvironmentVars.PROXY_SCHEME, identity(), builder::proxyScheme);
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
            new OkHttpPoster(
                buildProxy(config),
                buildProxyAuthenticator(config),
                Duration.of(10, ChronoUnit.SECONDS));
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
            .secondaryUserAgent(makeUserAgent(config));
    if (config.getEventsUri() != null) {
      eventsConfig = eventsConfig.endpoint(toURL(config.getEventsUri()));
    }
    if (config.isUseLicenseKey()) {
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
            .auditLoggingEnabled(config.auditLogging());
    if (config.getMetricsUri() != null) {
      metricConfig = metricConfig.endpoint(toURL(config.getMetricsUri()));
    }
    if (config.isUseLicenseKey()) {
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

  private static Proxy buildProxy(DaemonConfig config) {
    final String proxyHost = config.getProxyHost();
    final Integer proxyPort = config.getProxyPort();
    final String proxyScheme = config.getProxyScheme();

    if (proxyHost == null || proxyPort == null || proxyScheme == null) {
      return null;
    }

    if (proxyScheme.equalsIgnoreCase("https")) {
      // TODO See FIXME in OkHttpPoster
      logger.error("HTTPS proxy is not currently supported.");
      return null;
    }

    logger.info(
        "JFR HttpPoster configured to use "
            + proxyScheme
            + " proxy: "
            + proxyHost
            + ":"
            + proxyPort);
    return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
  }

  private static Authenticator buildProxyAuthenticator(DaemonConfig config) {
    final String proxyUser = config.getProxyUser();
    final String proxyPassword = config.getProxyPassword();

    if (proxyUser == null || proxyPassword == null) {
      return null;
    }

    logger.info("JFR HttpPoster configured with proxy user and proxy password.");
    return (route, response) ->
        response
            .request()
            .newBuilder()
            .header("Proxy-Authorization", Credentials.basic(proxyUser, proxyPassword))
            .build();
  }
}
