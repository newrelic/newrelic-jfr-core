/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.RecordedObjectValidators.*;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public class ThreadAllocationStatisticsMapper implements EventToMetric {
  private static final String SIMPLE_CLASS_NAME =
      ThreadAllocationStatisticsMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.ThreadAllocationStatistics";
  private static final String THREAD = "thread";
  private static final String THREAD_NAME = "thread.name";
  private static final String THREAD_OS_NAME = "thread.osName";
  private static final String ALLOCATED = "allocated";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long time = ev.getStartTime().toEpochMilli();
    double allocated = 0;
    if (hasField(ev, ALLOCATED, SIMPLE_CLASS_NAME)) {
      allocated = ev.getDouble(ALLOCATED);
    }
    RecordedThread t = null;
    if (hasField(ev, THREAD, SIMPLE_CLASS_NAME)) {
      t = ev.getValue(THREAD);
    }
    Attributes attr = new Attributes();
    if (t != null) {
      attr.put(THREAD_NAME, t.getJavaName());
      attr.put(THREAD_OS_NAME, t.getOSName());
    }

    return Collections.singletonList(
        new Gauge("jfr.ThreadAllocationStatistics.allocated", allocated, time, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
