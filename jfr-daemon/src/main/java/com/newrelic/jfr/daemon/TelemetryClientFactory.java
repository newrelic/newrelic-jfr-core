package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.EnvironmentVars.EVENTS_INGEST_URI;
import static com.newrelic.jfr.daemon.EnvironmentVars.INSERT_API_KEY;
import static com.newrelic.jfr.daemon.EnvironmentVars.METRICS_INGEST_URI;

import com.newrelic.telemetry.EventBatchSenderFactory;
import com.newrelic.telemetry.Java11HttpPoster;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatchSender;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

/** Builds the instance of the TelemetryClient used to send data to New Relic ingest endpoints. */
public class TelemetryClientFactory {

  public TelemetryClient build(DaemonConfig config) throws MalformedURLException {

    Supplier<HttpPoster> httpPosterCreator =
        () -> new Java11HttpPoster(Duration.of(10, ChronoUnit.SECONDS));

    String userAgent = "JFR-Daemon/" + config.getDaemonVersion();

    var metricBatchSender =
        MetricBatchSender.create(
            MetricBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
                .configureWith(config.getApiKey())
                .endpointWithPath(config.getMetricsUri().toURL())
                .secondaryUserAgent(userAgent)
                .auditLoggingEnabled(true)
                .build());

    var eventBatchSender =
        EventBatchSender.create(
            EventBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
                .configureWith(config.getApiKey())
                .endpointWithPath(config.getEventsUri().toURL())
                .auditLoggingEnabled(true)
                .secondaryUserAgent(userAgent)
                .build());

    return new TelemetryClient(metricBatchSender, null, eventBatchSender, null);
  }
}
