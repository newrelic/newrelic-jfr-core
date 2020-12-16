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
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

public class GarbageCollectionMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.GarbageCollection";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    var timestamp = ev.getStartTime().toEpochMilli();
    double longestPause = ev.getDouble("longestPause");

    var attr = new Attributes();
    var name = ev.getString("name");
    var cause = ev.getString("cause");
    if (name != null) {
      attr.put("name", name);
    }
    if (name != null) {
      attr.put("cause", cause);
    }

    return List.of(new Gauge("jfr.GarbageCollection.longestPause", longestPause, timestamp, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
