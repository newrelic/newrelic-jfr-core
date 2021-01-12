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
import java.util.Arrays;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

public class GCHeapSummaryMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.GCHeapSummary";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    long heapUsed = ev.getLong("heapUsed");
    RecordedObject heapSpace = ev.getValue("heapSpace");
    long committedSize = heapSpace.getLong("committedSize");
    long reservedSize = heapSpace.getLong("reservedSize");

    Attributes attr =
        new Attributes()
            .put("when", ev.getString("when"))
            .put("heapStart", heapSpace.getLong("start"))
            .put("committedEnd", heapSpace.getLong("committedEnd"))
            .put("reservedEnd", heapSpace.getLong("reservedEnd"));

    return Arrays.asList(
        new Gauge("jfr.GCHeapSummary.heapUsed", heapUsed, timestamp, attr),
        new Gauge("jfr.GCHeapSummary.heapCommittedSize", committedSize, timestamp, attr),
        new Gauge("jfr.GCHeapSummary.reservedSize", reservedSize, timestamp, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
