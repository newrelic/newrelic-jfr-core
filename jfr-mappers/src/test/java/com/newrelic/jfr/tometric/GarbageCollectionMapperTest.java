package com.newrelic.jfr.tometric;

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

class GarbageCollectionMapperTest {
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
    var name = "myName";
    var cause = "too huge";
    attr.put("name", name);
    attr.put("cause", cause);
    var longestPause = 21.77;
    var gauge1 = new Gauge("jfr.GarbageCollection.longestPause", longestPause, now, attr);
    List<Metric> expected = List.of(gauge1);

    var testClass = new GarbageCollectionMapper();
    var event = mock(RecordedEvent.class);

    when(event.getStartTime()).thenReturn(startTime);
    when(event.getDouble("longestPause")).thenReturn(longestPause);
    when(event.getString("name")).thenReturn(name);
    when(event.getString("cause")).thenReturn(cause);

    List<? extends Metric> result = testClass.apply(event);

    assertEquals(expected, result);
  }
}
