/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tometric;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public class ThreadAllocationStatisticsMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.ThreadAllocationStatistics";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long time = ev.getStartTime().toEpochMilli();
    double allocated = ev.getDouble("allocated");
    RecordedThread t = ev.getValue("thread");
    Attributes attr = new Attributes();
    if (t != null) {
      attr.put("thread.name", t.getJavaName()).put("thread.osName", t.getOSName());
    }

    return Collections.singletonList(
        new Gauge("jfr.ThreadAllocationStatistics.allocated", allocated, time, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
