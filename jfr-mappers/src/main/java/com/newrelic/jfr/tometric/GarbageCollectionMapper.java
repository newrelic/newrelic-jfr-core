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

public class GarbageCollectionMapper implements EventToMetric {
  public static final String SIMPLE_CLASS_NAME = GarbageCollectionMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.GarbageCollection";
  public static final String LONGEST_PAUSE = "longestPause";
  public static final String NAME = "name";
  public static final String CAUSE = "cause";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    double longestPause = 0;
    if (hasField(ev, LONGEST_PAUSE, SIMPLE_CLASS_NAME)) {
      longestPause = ev.getDouble(LONGEST_PAUSE);
    }
    Attributes attr = new Attributes();
    if (hasField(ev, NAME, SIMPLE_CLASS_NAME)) {
      attr.put(NAME, ev.getString(NAME));
    }
    if (hasField(ev, CAUSE, SIMPLE_CLASS_NAME)) {
      attr.put(CAUSE, ev.getString(CAUSE));
    }
    return Collections.singletonList(
        new Gauge("jfr.GarbageCollection.longestPause", longestPause, timestamp, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
