package com.newrelic.jfr.tometric;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThreadAllocationStatisticsMapperTest {

    @Test
    void testMapper() {
        var recordedThread = mock(RecordedThread.class);
        var threadName = "main";
        var threadOsName = "main";

        var recordedEvent = mock(RecordedEvent.class);
        var now = System.currentTimeMillis();
        var startTime = Instant.ofEpochMilli(now);
        var allocated = 1250229920d;

        var attr = new Attributes()
                .put("thread.name", threadName)
                .put("thread.osName", threadOsName);
        var gauge = new Gauge("jfr:ThreadAllocationStatistics.allocated", allocated, now, attr);
        var expected = List.of(gauge);

        var testClass = new ThreadAllocationStatisticsMapper();

        when(recordedThread.getJavaName()).thenReturn(threadName);
        when(recordedThread.getOSName()).thenReturn(threadOsName);

        when(recordedEvent.getStartTime()).thenReturn(startTime);
        when(recordedEvent.getDouble("allocated")).thenReturn(allocated);
        when(recordedEvent.getValue("thread")).thenReturn(recordedThread);

        var result = testClass.apply(recordedEvent);
        assertEquals(expected, result);
    }
}
