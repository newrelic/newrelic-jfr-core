package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.MetricBatch;

public class NewRelicTelemetrySender implements TelemetrySender {
  private final TelemetryClient client;

  public NewRelicTelemetrySender(TelemetryClient client) {
    this.client = client;
  }

  @Override
  public void sendBatch(MetricBatch metricBatch) {
    client.sendBatch(metricBatch);
  }

  @Override
  public void sendBatch(EventBatch eventBatch) {
    client.sendBatch(eventBatch);
  }
}
