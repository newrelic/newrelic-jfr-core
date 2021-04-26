package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.JFR_JVM_INFORMATION;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.JVM_PROPERTY;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.JVM_PROPERTY_VALUE;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.KEY;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class JVMSystemPropertyMapperTest {
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
  void testApply() {
    var key = "key1";
    var value = "value1";
    var startTime = Instant.now();

    var expectedAttrs = new Attributes();
    expectedAttrs.put(JVM_PROPERTY, key);
    expectedAttrs.put(JVM_PROPERTY_VALUE, value);

    var expectedEvent = new Event(JFR_JVM_INFORMATION, expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var mapper = new JVMSystemPropertyMapper(new AttributeValueSplitter());

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getString(KEY)).thenReturn(key);
    when(event.getString(VALUE)).thenReturn(value);

    var result = mapper.apply(event);

    assertEquals(expected, result);
  }
}
