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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public class ContextSwitchRateMapper implements EventToMetric {
  private static final String SIMPLE_CLASS_NAME = ContextSwitchRateMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.ThreadContextSwitchRate";
  public static final String SWITCH_RATE = "switchRate";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    double gaugeValue = 0;
    if (hasField(ev, SWITCH_RATE, SIMPLE_CLASS_NAME)) {
      gaugeValue = ev.getDouble(SWITCH_RATE);
    }
    return Collections.singletonList(
        new Gauge("jfr.ThreadContextSwitchRate", gaugeValue, timestamp, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.of(1, ChronoUnit.SECONDS));
  }
}
