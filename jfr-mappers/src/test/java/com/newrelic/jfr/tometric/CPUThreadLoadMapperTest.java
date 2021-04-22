package com.newrelic.jfr.tometric;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class CPUThreadLoadMapperTest {
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;

  @BeforeAll
  static void init() {
    recordedObjectValidatorsMockedStatic = Mockito.mockStatic(RecordedObjectValidators.class);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.hasField(
                    any(RecordedObject.class), anyString(), anyString()))
        .thenReturn(true);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.isRecordedObjectNull(
                    any(RecordedObject.class), anyString()))
        .thenReturn(false);
  }

  @AfterAll
  static void teardown() {
    recordedObjectValidatorsMockedStatic.close();
  }

  final String threadName = "moore";
  final double user = 75.6;
  final double system = 81.33;
  final Instant instant = Instant.now();
  final long timestamp = instant.toEpochMilli();

  @Test
  void testApplyWithThreadName() {

    Attributes attributes = new Attributes().put("thread.name", threadName);

    Metric gauge1 = new Gauge("jfr.ThreadCPULoad.user", user, timestamp, attributes);
    Metric gauge2 = new Gauge("jfr.ThreadCPULoad.system", system, timestamp, attributes);
    List<Metric> expected = List.of(gauge1, gauge2);

    RecordedEvent event = mock(RecordedEvent.class);
    RecordedThread recordedThread = mock(RecordedThread.class);

    when(event.getStartTime()).thenReturn(instant);
    when(event.getDouble("user")).thenReturn(user);
    when(event.getDouble("system")).thenReturn(system);
    when(event.getValue("eventThread")).thenReturn(recordedThread);
    when(recordedThread.getJavaName()).thenReturn(threadName);

    CPUThreadLoadMapper mapper = new CPUThreadLoadMapper();

    List<? extends Metric> result = mapper.apply(event);

    assertEquals(expected, result);
  }

  @Test
  void testApplyNoThreadName() {

    Object recordedThread = new Object(); // not a recorded thread

    RecordedEvent event = mock(RecordedEvent.class);
    when(event.getValue("eventThread")).thenReturn(recordedThread);

    CPUThreadLoadMapper mapper = new CPUThreadLoadMapper();

    List<? extends Metric> result = mapper.apply(event);
    assertEquals(List.of(), result);
  }
}
