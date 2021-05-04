package com.newrelic.jfr;

import static com.newrelic.jfr.RecordedObjectValidators.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class RecordedObjectValidatorsTest {
  public static final String SIMPLE_CLASS_NAME = RecordedObjectValidatorsTest.class.getSimpleName();

  @Test
  void testValidField() {
    var event = mock(RecordedEvent.class);
    var fieldToValidate = "foo";
    var expectedFieldResult = "bar";

    when(event.getString(fieldToValidate)).thenReturn(expectedFieldResult);
    when(event.hasField(fieldToValidate)).thenReturn(true);

    var result = hasField(event, fieldToValidate, SIMPLE_CLASS_NAME);
    var actualFieldResult = event.getString(fieldToValidate);

    assertTrue(result);
    assertEquals(expectedFieldResult, actualFieldResult);
  }

  @Test
  void testInvalidField() {
    var event = mock(RecordedEvent.class);
    var fieldToValidate = "foo";

    when(event.hasField(fieldToValidate)).thenReturn(false);

    var result = hasField(event, fieldToValidate, SIMPLE_CLASS_NAME);

    assertFalse(result);
  }

  @Test
  void testNonNullObject() {
    RecordedEvent event = mock(RecordedEvent.class);

    var realEventResult = isRecordedObjectNull(event, SIMPLE_CLASS_NAME);

    assertFalse(realEventResult);
  }

  @Test
  void testNullObject() {
    RecordedEvent nullEvent = null;

    var nullEventResult = isRecordedObjectNull(nullEvent, SIMPLE_CLASS_NAME);

    assertTrue(nullEventResult);
  }
}
