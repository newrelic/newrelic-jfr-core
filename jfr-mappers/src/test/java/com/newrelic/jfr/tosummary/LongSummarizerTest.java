package com.newrelic.jfr.tosummary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class LongSummarizerTest {
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
  void testAccept() {
    var ev = mock(RecordedEvent.class);

    var fieldName = "theGoodData";
    when(ev.getLong(fieldName)).thenReturn(11L, 12L);

    var testClass = new LongSummarizer(fieldName);
    testClass.accept(ev);
    testClass.accept(ev);

    assertEquals(2, testClass.getCount());
    assertEquals(23, testClass.getSum());
    assertEquals(11, testClass.getMin());
    assertEquals(12, testClass.getMax());
  }

  @Test
  void testDefaultState() {
    var fieldName = "theGoodData";

    var testClass = new LongSummarizer(fieldName);

    assertEquals(0, testClass.getCount());
    assertEquals(0, testClass.getSum());
    assertEquals(Long.MAX_VALUE, testClass.getMin());
    assertEquals(Long.MIN_VALUE, testClass.getMax());
  }

  @Test
  void testReset() {
    var ev = mock(RecordedEvent.class);

    var fieldName = "theGoodData";
    when(ev.getLong(fieldName)).thenReturn(101L, 102L);

    var testClass = new LongSummarizer(fieldName);
    testClass.accept(ev);
    testClass.reset();
    testClass.accept(ev);

    assertEquals(1, testClass.getCount());
    assertEquals(102, testClass.getSum());
    assertEquals(102, testClass.getMin());
    assertEquals(102, testClass.getMax());
  }
}
