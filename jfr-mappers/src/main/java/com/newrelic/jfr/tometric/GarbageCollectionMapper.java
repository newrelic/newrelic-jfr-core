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

public class GarbageCollectionMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.GarbageCollection";
  public static final String LONGEST_PAUSE = "longestPause";
  public static final String NAME = "name";
  public static final String CAUSE = "cause";
  public static final String JFR_GARBAGE_COLLECTION_LONGEST_PAUSE =
      "jfr.GarbageCollection.longestPause";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    double longestPause = 0;
    if (ev.hasField(LONGEST_PAUSE)) {
      longestPause = ev.getDouble(LONGEST_PAUSE);
    }
    Attributes attr = new Attributes();
    if (ev.hasField(NAME)) {
      attr.put(NAME, ev.getString(NAME));
    }
    if (ev.hasField(CAUSE)) {
      attr.put(CAUSE, ev.getString(CAUSE));
    }
    return Collections.singletonList(
        new Gauge(JFR_GARBAGE_COLLECTION_LONGEST_PAUSE, longestPause, timestamp, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
