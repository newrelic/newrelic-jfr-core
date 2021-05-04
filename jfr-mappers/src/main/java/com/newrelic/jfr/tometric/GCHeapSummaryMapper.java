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
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

public class GCHeapSummaryMapper implements EventToMetric {
  public static final String SIMPLE_CLASS_NAME = GCHeapSummaryMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.GCHeapSummary";
  public static final String HEAP_USED = "heapUsed";
  public static final String HEAP_SPACE = "heapSpace";
  public static final String COMMITTED_SIZE = "committedSize";
  public static final String RESERVED_SIZE = "reservedSize";
  public static final String START = "start";
  public static final String HEAP_START = "heapStart";
  public static final String WHEN = "when";
  public static final String COMMITTED_END = "committedEnd";
  public static final String RESERVED_END = "reservedEnd";
  public static final String JFR_GC_HEAP_SUMMARY_HEAP_COMMITTED_SIZE =
      "jfr.GCHeapSummary.heapCommittedSize";
  public static final String JFR_GC_HEAP_SUMMARY_RESERVED_SIZE = "jfr.GCHeapSummary.reservedSize";
  public static final String JFR_GC_HEAP_SUMMARY_HEAP_USED = "jfr.GCHeapSummary.heapUsed";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    long heapUsed = 0;
    if (hasField(ev, HEAP_USED, SIMPLE_CLASS_NAME)) {
      heapUsed = ev.getLong(HEAP_USED);
    }
    List<Gauge> list = new ArrayList<>();
    Attributes attr = new Attributes();
    RecordedObject heapSpace = null;
    if (hasField(ev, HEAP_SPACE, SIMPLE_CLASS_NAME)) {
      heapSpace = ev.getValue(HEAP_SPACE);
    }
    if (!isRecordedObjectNull(heapSpace, SIMPLE_CLASS_NAME)) {
      long committedSize = 0;
      if (hasField(heapSpace, COMMITTED_SIZE, SIMPLE_CLASS_NAME)) {
        committedSize = heapSpace.getLong(COMMITTED_SIZE);
      }
      long reservedSize = 0;
      if (hasField(heapSpace, RESERVED_SIZE, SIMPLE_CLASS_NAME)) {
        reservedSize = heapSpace.getLong(RESERVED_SIZE);
      }
      if (hasField(heapSpace, WHEN, SIMPLE_CLASS_NAME)) {
        attr.put(WHEN, ev.getString(WHEN));
      }
      if (hasField(heapSpace, START, SIMPLE_CLASS_NAME)) {
        attr.put(HEAP_START, heapSpace.getLong(START));
      }
      if (hasField(heapSpace, COMMITTED_END, SIMPLE_CLASS_NAME)) {
        attr.put(COMMITTED_END, heapSpace.getLong(COMMITTED_END));
      }
      if (hasField(heapSpace, RESERVED_END, SIMPLE_CLASS_NAME)) {
        attr.put(RESERVED_END, heapSpace.getLong(RESERVED_END));
      }
      list.add(new Gauge(JFR_GC_HEAP_SUMMARY_HEAP_COMMITTED_SIZE, committedSize, timestamp, attr));
      list.add(new Gauge(JFR_GC_HEAP_SUMMARY_RESERVED_SIZE, reservedSize, timestamp, attr));
    }
    list.add(new Gauge(JFR_GC_HEAP_SUMMARY_HEAP_USED, heapUsed, timestamp, attr));
    return list;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
