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
  public static final String JVM_USER = "jvmUser";
  public static final String JVM_SYSTEM = "jvmSystem";
  public static final String MACHINE_TOTAL = "machineTotal";
  public static final String JFR_CPU_LOAD_JVM_USER = "jfr.CPULoad.jvmUser";
  public static final String JFR_CPU_LOAD_JVM_SYSTEM = "jfr.CPULoad.jvmSystem";
  public static final String JFR_CPU_LOAD_MACHINE_TOTAL = "jfr.CPULoad.machineTotal";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    double jvmUserGaugeValue = 0;
    if (ev.hasField(JVM_USER)) {
      jvmUserGaugeValue = ev.getDouble(JVM_USER);
    }
    double jvmSystemGaugeValue = 0;
    if (ev.hasField(JVM_SYSTEM)) {
      jvmSystemGaugeValue = ev.getDouble(JVM_SYSTEM);
    }
    double machineTotalGaugeValue = 0;
    if (ev.hasField(MACHINE_TOTAL)) {
      machineTotalGaugeValue = ev.getDouble(MACHINE_TOTAL);
    }
    return Arrays.asList(
        new Gauge(JFR_CPU_LOAD_JVM_USER, jvmUserGaugeValue, timestamp, attr),
        new Gauge(JFR_CPU_LOAD_JVM_SYSTEM, jvmSystemGaugeValue, timestamp, attr),
        new Gauge(JFR_CPU_LOAD_MACHINE_TOTAL, machineTotalGaugeValue, timestamp, attr));
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
