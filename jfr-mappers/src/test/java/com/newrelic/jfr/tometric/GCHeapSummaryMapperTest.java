package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.COMMITTED_END;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.COMMITTED_SIZE;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.HEAP_SPACE;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.HEAP_START;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.HEAP_USED;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.JFR_GC_HEAP_SUMMARY_HEAP_COMMITTED_SIZE;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.JFR_GC_HEAP_SUMMARY_HEAP_USED;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.JFR_GC_HEAP_SUMMARY_RESERVED_SIZE;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.RESERVED_END;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.RESERVED_SIZE;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.START;
import static com.newrelic.jfr.tometric.GCHeapSummaryMapper.WHEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class GCHeapSummaryMapperTest {
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

  @Test
  void testMapper() {
    var now = System.currentTimeMillis();
    Instant startTime = Instant.ofEpochMilli(now);
    var attr = new Attributes();
    long heapUsed = 300;
    long heapCommittedSize = 500;
    long reservedSize = 200;
    long heapStart = 123;
    long committedEnd = 321;
    long reservedEnd = 456;
    String when = "when";
    attr.put(WHEN, when);
    attr.put(HEAP_START, heapStart);
    attr.put(COMMITTED_END, committedEnd);
    attr.put(RESERVED_END, reservedEnd);

    var gauge1 = new Gauge(JFR_GC_HEAP_SUMMARY_HEAP_COMMITTED_SIZE, heapCommittedSize, now, attr);
    var gauge2 = new Gauge(JFR_GC_HEAP_SUMMARY_RESERVED_SIZE, reservedSize, now, attr);
    var gauge3 = new Gauge(JFR_GC_HEAP_SUMMARY_HEAP_USED, heapUsed, now, attr);
    List<Metric> expected = List.of(gauge1, gauge2, gauge3);

    var testClass = new GCHeapSummaryMapper();

    var recordedObject = mock(RecordedObject.class);
    when(recordedObject.getLong(COMMITTED_SIZE)).thenReturn(heapCommittedSize);
    when(recordedObject.getLong(RESERVED_SIZE)).thenReturn(reservedSize);
    when(recordedObject.getLong(START)).thenReturn(heapStart);
    when(recordedObject.getLong(COMMITTED_END)).thenReturn(committedEnd);
    when(recordedObject.getLong(RESERVED_END)).thenReturn(reservedEnd);

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getLong(HEAP_USED)).thenReturn(heapUsed);
    when(event.getValue(HEAP_SPACE)).thenReturn(recordedObject);
    when(event.getString(WHEN)).thenReturn(when);

    List<? extends Metric> result = testClass.apply(event);

    assertEquals(expected, result);
  }
}
