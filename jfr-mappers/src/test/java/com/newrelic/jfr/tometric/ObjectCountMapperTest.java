package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.tometric.ObjectCountMapper.CLASS_ATTR;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ObjectCountMapperTest {
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
    var className = "java.lang.Object";
    var event = mock(RecordedEvent.class);
    var recordedClass = mock(RecordedClass.class);
    var count = 42;
    var totalSize = 1500L;
    var eventStartTime = Instant.now().toEpochMilli();
    var attr = new Attributes().put(CLASS_ATTR, className);

    var expectedCountMetric =
        new Gauge(ObjectCountMapper.COUNT_METRIC_NAME, count, eventStartTime, attr);
    var expectedTotalSizeMetric =
        new Gauge(ObjectCountMapper.TOTAL_SIZE_METRIC_NAME, totalSize, eventStartTime, attr);
    Set<Metric> expected = Set.of(expectedCountMetric, expectedTotalSizeMetric);
    var testClass = new ObjectCountMapper();

    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getClass(ObjectCountMapper.OBJECT_CLASS)).thenReturn(recordedClass);
    when(recordedClass.getName()).thenReturn(className);
    when(event.getLong(ObjectCountMapper.COUNT_FIELD)).thenReturn((long) count);
    when(event.getLong(ObjectCountMapper.TOTAL_SIZE_FIELD)).thenReturn(totalSize);

    final List<Gauge> result = testClass.apply(event);

    // transforming the received list to a set to disregard ordering
    assertEquals(expected, new HashSet<>(result));
  }
}
