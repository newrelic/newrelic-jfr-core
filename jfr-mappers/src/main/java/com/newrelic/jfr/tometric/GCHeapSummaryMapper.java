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
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

public class GCHeapSummaryMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.GCHeapSummary";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    long heapUsed = ev.getLong("heapUsed");
    List<Gauge> list = new ArrayList<>();
    Attributes attr = new Attributes();
    RecordedObject heapSpace = ev.getValue("heapSpace");
    if (heapSpace != null) {
      long committedSize = heapSpace.getLong("committedSize");
      long reservedSize = heapSpace.getLong("reservedSize");
      attr.put("when", ev.getString("when"));
      attr.put("heapStart", heapSpace.getLong("start"));
      attr.put("committedEnd", heapSpace.getLong("committedEnd"));
      attr.put("reservedEnd", heapSpace.getLong("reservedEnd"));
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
