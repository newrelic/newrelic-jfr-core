package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.tometric.CPUThreadLoadMapper.JFR_THREAD_CPU_LOAD_SYSTEM;
import static com.newrelic.jfr.tometric.CPUThreadLoadMapper.JFR_THREAD_CPU_LOAD_USER;
import static com.newrelic.jfr.tometric.CPUThreadLoadMapper.SYSTEM;
import static com.newrelic.jfr.tometric.CPUThreadLoadMapper.THREAD_NAME;
import static com.newrelic.jfr.tometric.CPUThreadLoadMapper.USER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class CPUThreadLoadMapperTest {
  private static final String EVENT_THREAD = "eventThread";

  final String threadName = "moore";
  final double user = 75.6;
  final double system = 81.33;
  final Instant instant = Instant.now();
  final long timestamp = instant.toEpochMilli();

  @Test
  void testApplyWithThreadName() {

    Attributes attributes = new Attributes().put(THREAD_NAME, threadName);

    Metric gauge1 = new Gauge(JFR_THREAD_CPU_LOAD_USER, user, timestamp, attributes);
    Metric gauge2 = new Gauge(JFR_THREAD_CPU_LOAD_SYSTEM, system, timestamp, attributes);
    List<Metric> expected = List.of(gauge1, gauge2);

    RecordedEvent event = mock(RecordedEvent.class);
    RecordedThread recordedThread = mock(RecordedThread.class);

    when(event.getStartTime()).thenReturn(instant);
    when(event.getDouble(USER)).thenReturn(user);
    when(event.getDouble(SYSTEM)).thenReturn(system);
    when(event.getValue(EVENT_THREAD)).thenReturn(recordedThread);
    when(recordedThread.getJavaName()).thenReturn(threadName);

    CPUThreadLoadMapper mapper = new CPUThreadLoadMapper();

    List<? extends Metric> result = mapper.apply(event);

    assertEquals(expected, result);
  }

  @Test
  void testApplyNoThreadName() {

    Object recordedThread = new Object(); // not a recorded thread

    RecordedEvent event = mock(RecordedEvent.class);
    when(event.getValue(EVENT_THREAD)).thenReturn(recordedThread);

    CPUThreadLoadMapper mapper = new CPUThreadLoadMapper();

    List<? extends Metric> result = mapper.apply(event);
    assertEquals(List.of(), result);
  }
}
