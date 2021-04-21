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
  private static final String SIMPLE_CLASS_NAME = GCHeapSummaryMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.GCHeapSummary";
  private static final String HEAP_USED = "heapUsed";
  private static final String HEAP_SPACE = "heapSpace";
  private static final String COMMITTED_SIZE = "committedSize";
  private static final String RESERVED_SIZE = "reservedSize";
  private static final String START = "start";
  private static final String HEAP_START = "heapStart";
  private static final String WHEN = "when";
  private static final String COMMITTED_END = "committedEnd";
  private static final String RESERVED_END = "reservedEnd";

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
    if (heapSpace != null) {
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
      list.add(new Gauge("jfr.GCHeapSummary.heapCommittedSize", committedSize, timestamp, attr));
      list.add(new Gauge("jfr.GCHeapSummary.reservedSize", reservedSize, timestamp, attr));
    }
    list.add(new Gauge("jfr.GCHeapSummary.heapUsed", heapUsed, timestamp, attr));
    return list;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
