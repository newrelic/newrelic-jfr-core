package com.newrelic.jfr.daemon.buffer;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.MetricBuffer;

/** Works around the fact that MetricBuffer does not yet expose a count */
public class CountingMetricBuffer {

  private final MetricBuffer delegate;
  private int size = 0;

  public CountingMetricBuffer(Attributes commonAttributes) {
    this(new MetricBuffer(commonAttributes));
  }

  public CountingMetricBuffer(MetricBuffer delegate) {
    this.delegate = delegate;
  }

  public void addMetric(Metric metric) {
    delegate.addMetric(metric);
    size++;
  }

  public MetricBatch createBatch() {
    size = 0;
    return delegate.createBatch();
  }

  public int size() {
    return size;
  }
}
