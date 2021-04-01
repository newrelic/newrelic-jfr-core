package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.MetricBatch;

public interface TelemetrySender {
  void sendBatch(MetricBatch metricBatch);

  void sendBatch(EventBatch eventBatch);
}
