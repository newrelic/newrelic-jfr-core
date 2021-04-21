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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public class OverallCPULoadMapper implements EventToMetric {
  private static final String SIMPLE_CLASS_NAME = OverallCPULoadMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.CPULoad";
  public static final String JVM_USER = "jvmUser";
  public static final String JVM_SYSTEM = "jvmSystem";
  public static final String MACHINE_TOTAL = "machineTotal";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    double jvmUserGaugeValue = 0;
    if (hasField(ev, JVM_USER, SIMPLE_CLASS_NAME)) {
      jvmUserGaugeValue = ev.getDouble(JVM_USER);
    }
    double jvmSystemGaugeValue = 0;
    if (hasField(ev, JVM_SYSTEM, SIMPLE_CLASS_NAME)) {
      jvmSystemGaugeValue = ev.getDouble(JVM_SYSTEM);
    }
    double machineTotalGaugeValue = 0;
    if (hasField(ev, MACHINE_TOTAL, SIMPLE_CLASS_NAME)) {
      machineTotalGaugeValue = ev.getDouble(MACHINE_TOTAL);
    }
    return Arrays.asList(
        new Gauge("jfr.CPULoad.jvmUser", jvmUserGaugeValue, timestamp, attr),
        new Gauge("jfr.CPULoad.jvmSystem", jvmSystemGaugeValue, timestamp, attr),
        new Gauge("jfr.CPULoad.machineTotal", machineTotalGaugeValue, timestamp, attr));
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
