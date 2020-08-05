package com.newrelic.jfr.daemon.buffer;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.EventBuffer;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.MetricBuffer;

/**
 * This class buffers metrics and event telemetry in memory until batches are created for upload.
 * Calling createMetricBatch or createEventBatch will return a batch that can be sent with the
 * TelemetryClient and will drain the underlying buffer delegate.
 */
public class BufferedTelemetry {
  private final CountingMetricBuffer metrics;
  private final CountingEventBuffer events;

  public BufferedTelemetry(MetricBuffer metrics, EventBuffer events) {
    this(new CountingMetricBuffer(metrics), new CountingEventBuffer(events));
  }

  public BufferedTelemetry(CountingMetricBuffer metrics, CountingEventBuffer events) {
    this.metrics = metrics;
    this.events = events;
  }

  public static BufferedTelemetry create(Attributes attributes) {
    var metrics = new CountingMetricBuffer(attributes);
    var events = new CountingEventBuffer(attributes);
    return new BufferedTelemetry(metrics, events);
  }

  public void addMetric(Metric metric) {
    metrics.addMetric(metric);
  }

  public void addEvent(Event event) {
    events.addEvent(event);
  }

  public MetricBatch createMetricBatch() {
    return metrics.createBatch();
  }

  public int getTotalSize() {
    return metrics.size() + events.size();
  }

  public EventBatch createEventBatch() {
    return events.createBatch();
  }
}
