package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.EventBatchSenderFactory;
import com.newrelic.telemetry.Java11HttpPoster;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import java.net.MalformedURLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/** Builds the instance of the TelemetryClient used to send data to New Relic ingest endpoints. */
public class TelemetryClientFactory {

  private final Supplier<HttpPoster> httpPosterCreator =
      () -> new Java11HttpPoster(Duration.of(10, ChronoUnit.SECONDS));

  public TelemetryClient build(DaemonConfig config) throws MalformedURLException {
    var metricBatchSender = buildMetricBatchSender(config);
    var eventBatchSender = buildEventBatchSender(config);
    return new TelemetryClient(metricBatchSender, null, eventBatchSender, null);
  }

  private EventBatchSender buildEventBatchSender(DaemonConfig config) throws MalformedURLException {
    var eventsConfig =
        EventBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
            .configureWith(config.getApiKey())
            .auditLoggingEnabled(true)
            .secondaryUserAgent(makeUserAgent(config));
    if (config.getEventsUri() != null) {
      eventsConfig = eventsConfig.endpointWithPath(config.getEventsUri().toURL());
    }
    return EventBatchSender.create(eventsConfig.build());
  }

  private MetricBatchSender buildMetricBatchSender(DaemonConfig config)
      throws MalformedURLException {
    var metricConfig =
        MetricBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
            .configureWith(config.getApiKey())
            .secondaryUserAgent(makeUserAgent(config))
            .auditLoggingEnabled(true);
    if (config.getMetricsUri() != null) {
      metricConfig = metricConfig.endpointWithPath(config.getMetricsUri().toURL());
    }
    return MetricBatchSender.create(metricConfig.build());
  }

  private String makeUserAgent(DaemonConfig config) {
    return "JFR-Daemon/" + config.getDaemonVersion();
  }
}
