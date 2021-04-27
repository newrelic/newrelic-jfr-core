package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.tometric.AllocationRequiringGCMapper.JFR_ALLOCATION_REQUIRING_GC_ALLOCATION_SIZE;
import static com.newrelic.jfr.tometric.AllocationRequiringGCMapper.SIZE;
import static com.newrelic.jfr.tometric.AllocationRequiringGCMapper.THREAD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class AllocationRequiringGCMapperTest {
  private static final String EVENT_THREAD = "eventThread";

  @Test
  void testMapper() {
    var recordedThread = mock(RecordedThread.class);
    var eventThread = "Thread-13";

    var recordedEvent = mock(RecordedEvent.class);
    var now = System.currentTimeMillis();
    var end = now + 1;
    var startTime = Instant.ofEpochMilli(now);
    var endTime = Instant.ofEpochMilli(end);
    var size = 32784L;

    var attr = new Attributes().put(THREAD_NAME, eventThread);
    var gauge = new Gauge(JFR_ALLOCATION_REQUIRING_GC_ALLOCATION_SIZE, size, now, attr);
    var expected = List.of(gauge);

    var testClass = new AllocationRequiringGCMapper();

    when(recordedThread.getJavaName()).thenReturn(eventThread);

    when(recordedEvent.getStartTime()).thenReturn(startTime);
    when(recordedEvent.getEndTime()).thenReturn(endTime);
    when(recordedEvent.getValue(EVENT_THREAD)).thenReturn(recordedThread);
    when(recordedEvent.getLong(SIZE)).thenReturn(size);
    when(recordedEvent.hasField(EVENT_THREAD)).thenReturn(true);
    when(recordedEvent.hasField(SIZE)).thenReturn(true);

    var result = testClass.apply(recordedEvent);
    assertEquals(expected, result);
  }
}
