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

//  private final String apiKey;
//  private final String metricsUri;
//  private final String eventsUri;
//
//  public TelemetryClientFactory() {
//    this(
//        System.getenv(INSERT_API_KEY),
//        System.getenv(METRICS_INGEST_URI),
//        System.getenv(EVENTS_INGEST_URI));
//  }

//  public TelemetryClientFactory(String apiKey, String metricsUri, String eventsUri) {
//    this.apiKey = apiKey;
//    this.metricsUri = metricsUri;
//    this.eventsUri = eventsUri;
//  }

  public TelemetryClient build(DaemonConfig config) throws MalformedURLException {

    Supplier<HttpPoster> httpPosterCreator =
        () -> new Java11HttpPoster(Duration.of(10, ChronoUnit.SECONDS));

    var metricBatchSender =
        MetricBatchSender.create(
            MetricBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
                .configureWith(config.getApiKey())
                .endpointWithPath(config.getMetricsUri().toURL())
                .auditLoggingEnabled(true)
                .build());

    var eventBatchSender =
        EventBatchSender.create(
            EventBatchSenderFactory.fromHttpImplementation(httpPosterCreator)
                .configureWith(config.getApiKey())
                .endpointWithPath(config.getEventsUri().toURL())
                .auditLoggingEnabled(true)
                .build());

    return new TelemetryClient(metricBatchSender, null, eventBatchSender, null);
  }
}
