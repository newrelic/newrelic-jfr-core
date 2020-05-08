package com.newrelic.jfr.tometric;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OverallCPULoadMapperTest {

    @Test
    void testMap() {
        var now = System.currentTimeMillis();
        Instant startTime = Instant.ofEpochMilli(now);
        var attr = new Attributes();
        var jvmUser = 21.77;
        var jvmSystem = 22.98;
        var machineTotal = 1203987.22;
        var gauge1 = new Gauge("jfr:CPULoad.jvmUser", jvmUser, now, attr);
        var gauge2 = new Gauge("jfr:CPULoad.jvmSystem", jvmSystem, now, attr);
        var gauge3 = new Gauge("jfr:CPULoad.machineTotal", machineTotal, now, attr);
        List<Metric> expected = List.of(gauge1, gauge2, gauge3);

        var testClass = new OverallCPULoadMapper();
        var event = mock(RecordedEvent.class);

        when(event.getStartTime()).thenReturn(startTime);
        when(event.getDouble("jvmUser")).thenReturn(jvmUser);
        when(event.getDouble("jvmSystem")).thenReturn(jvmSystem);
        when(event.getDouble("machineTotal")).thenReturn(machineTotal);

        List<? extends Metric> result = testClass.apply(event);

        assertEquals(expected, result);
    }

    @Test
    void hasDuration() {
        var testClass = new OverallCPULoadMapper();
        assertFalse(testClass.getPollingDuration().isEmpty());
    }

}