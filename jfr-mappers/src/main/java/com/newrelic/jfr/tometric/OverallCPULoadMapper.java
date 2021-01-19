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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public class OverallCPULoadMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.CPULoad";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    return Arrays.asList(
        new Gauge("jfr.CPULoad.jvmUser", ev.getDouble("jvmUser"), timestamp, attr),
        new Gauge("jfr.CPULoad.jvmSystem", ev.getDouble("jvmSystem"), timestamp, attr),
        new Gauge("jfr.CPULoad.machineTotal", ev.getDouble("machineTotal"), timestamp, attr));
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
