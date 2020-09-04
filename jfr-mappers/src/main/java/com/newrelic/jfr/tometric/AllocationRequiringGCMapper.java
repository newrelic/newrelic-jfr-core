/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tometric;

import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

public class AllocationRequiringGCMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.AllocationRequiringGC";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    var attr = new Attributes();
    var timestamp = ev.getStartTime().toEpochMilli();
    var threadName = Workarounds.getThreadName(ev);
    threadName.ifPresent(thread -> attr.put("thread.name", thread));
    return List.of(
        new Gauge("jfr:AllocationRequiringGC.allocationSize", ev.getLong("size"), timestamp, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
