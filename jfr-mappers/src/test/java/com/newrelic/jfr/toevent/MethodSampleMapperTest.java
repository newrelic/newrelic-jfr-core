package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.MethodSampleMapper.JFR_METHOD_SAMPLE;
import static com.newrelic.jfr.toevent.MethodSampleMapper.SAMPLED_THREAD;
import static com.newrelic.jfr.toevent.MethodSampleMapper.STACK_TRACE;
import static com.newrelic.jfr.toevent.MethodSampleMapper.STATE;
import static com.newrelic.jfr.toevent.MethodSampleMapper.THREAD_NAME;
import static com.newrelic.jfr.toevent.MethodSampleMapper.THREAD_STATE;
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
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class MethodSampleMapperTest {
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
    var threadName = "santiago";
    var threadState = "almost_asleep";
    var stackTrace =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":[]}";
    var startTime = Instant.now();
    var expectedAttrs =
        new Attributes()
            .put(THREAD_NAME, threadName)
            .put(THREAD_STATE, threadState)
            .put(STACK_TRACE, stackTrace);
    var expectedEvent = new Event(JFR_METHOD_SAMPLE, expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var stack = mock(RecordedStackTrace.class);
    var sampledThread = mock(RecordedThread.class);

    when(stack.getFrames()).thenReturn(List.of());
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getStackTrace()).thenReturn(stack);
    when(event.getThread(SAMPLED_THREAD)).thenReturn(sampledThread);
    when(event.getString(STATE)).thenReturn(threadState);
    when(sampledThread.getJavaName()).thenReturn(threadName);

    var mapper = MethodSampleMapper.forExecutionSample();

    var result = mapper.apply(event);
    assertEquals(expected, result);
  }

  @Test
  void testApplyButNoTrace() {
    var event = mock(RecordedEvent.class);
    when(event.getStackTrace()).thenReturn(null);
    var mapper = MethodSampleMapper.forExecutionSample();

    var result = mapper.apply(event);
    assertEquals(List.of(), result);
  }
}
