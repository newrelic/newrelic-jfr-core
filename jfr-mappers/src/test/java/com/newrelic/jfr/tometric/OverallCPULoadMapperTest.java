package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.tometric.OverallCPULoadMapper.JFR_CPU_LOAD_JVM_SYSTEM;
import static com.newrelic.jfr.tometric.OverallCPULoadMapper.JFR_CPU_LOAD_JVM_USER;
import static com.newrelic.jfr.tometric.OverallCPULoadMapper.JFR_CPU_LOAD_MACHINE_TOTAL;
import static com.newrelic.jfr.tometric.OverallCPULoadMapper.JVM_SYSTEM;
import static com.newrelic.jfr.tometric.OverallCPULoadMapper.JVM_USER;
import static com.newrelic.jfr.tometric.OverallCPULoadMapper.MACHINE_TOTAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class OverallCPULoadMapperTest {

  @Test
  void testMap() {
    var now = System.currentTimeMillis();
    Instant startTime = Instant.ofEpochMilli(now);
    var attr = new Attributes();
    var jvmUser = 21.77;
    var jvmSystem = 22.98;
    var machineTotal = 1203987.22;
    var gauge1 = new Gauge(JFR_CPU_LOAD_JVM_USER, jvmUser, now, attr);
    var gauge2 = new Gauge(JFR_CPU_LOAD_JVM_SYSTEM, jvmSystem, now, attr);
    var gauge3 = new Gauge(JFR_CPU_LOAD_MACHINE_TOTAL, machineTotal, now, attr);
    List<Metric> expected = List.of(gauge1, gauge2, gauge3);

    var testClass = new OverallCPULoadMapper();
    var event = mock(RecordedEvent.class);

    when(event.getStartTime()).thenReturn(startTime);
    when(event.getDouble(JVM_USER)).thenReturn(jvmUser);
    when(event.getDouble(JVM_SYSTEM)).thenReturn(jvmSystem);
    when(event.getDouble(MACHINE_TOTAL)).thenReturn(machineTotal);
    when(event.hasField(JVM_USER)).thenReturn(true);
    when(event.hasField(JVM_SYSTEM)).thenReturn(true);
    when(event.hasField(MACHINE_TOTAL)).thenReturn(true);

    List<? extends Metric> result = testClass.apply(event);

    assertEquals(expected, result);
  }

  @Test
  void hasDuration() {
    var testClass = new OverallCPULoadMapper();
    assertFalse(testClass.getPollingDuration().isEmpty());
  }
}
