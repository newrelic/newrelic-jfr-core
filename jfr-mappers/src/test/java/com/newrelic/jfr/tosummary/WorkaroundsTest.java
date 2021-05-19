package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.Workarounds.EVENT_THREAD;
import static com.newrelic.jfr.Workarounds.SUCCEDED_TYPO;
import static com.newrelic.jfr.Workarounds.SUCCEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.jfr.Workarounds;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class WorkaroundsTest {
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
  void testGetThreadNameHappyPath() {
    String threadName = "berger";
    var ev = mock(RecordedEvent.class);
    var thread = mock(RecordedThread.class);
    when(thread.getJavaName()).thenReturn(threadName);
    when(ev.getValue(EVENT_THREAD)).thenReturn(thread);
    assertEquals(threadName, Workarounds.getThreadName(ev).get());
  }

  @Test
  void testGetThreadNameWrongType() throws Exception {
    var ev = mock(RecordedEvent.class);
    when(ev.getValue(EVENT_THREAD)).thenReturn(new Object[] {"aa", 21, "bbbbbbb"});
    assertTrue(Workarounds.getThreadName(ev).isEmpty());
  }

  @Test
  public void testGetSucceededCorrect() throws Exception {
    var ev = mock(RecordedEvent.class);
    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.hasField(
                    any(RecordedObject.class), eq(SUCCEEDED), anyString()))
        .thenReturn(true);

    when(ev.hasField(SUCCEEDED)).thenReturn(true);
    when(ev.getBoolean(SUCCEEDED)).thenReturn(true);
    assertTrue(Workarounds.getSucceeded(ev));
  }

  @Test
  public void testGetSucceededIncorrect() throws Exception {
    var ev = mock(RecordedEvent.class);
    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.hasField(
                    any(RecordedObject.class), eq(SUCCEEDED), anyString()))
        .thenReturn(false);

    when(ev.hasField(SUCCEEDED)).thenReturn(false);
    when(ev.getBoolean(SUCCEDED_TYPO)).thenReturn(true);
    assertTrue(Workarounds.getSucceeded(ev));
  }
}
