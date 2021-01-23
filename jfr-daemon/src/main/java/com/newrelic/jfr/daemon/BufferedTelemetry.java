/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.events.EventBuffer;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.MetricBuffer;

public class BufferedTelemetry {
  private final MetricBuffer metrics;
  private final EventBuffer events;

  public BufferedTelemetry(MetricBuffer metrics, EventBuffer events) {
    this.metrics = metrics;
    this.events = events;
  }

  public static BufferedTelemetry create(Attributes attributes) {
    MetricBuffer metrics = new MetricBuffer(attributes);
    EventBuffer events = new EventBuffer(attributes);
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

  public EventBatch createEventBatch() {
    return events.createBatch();
  }
}
