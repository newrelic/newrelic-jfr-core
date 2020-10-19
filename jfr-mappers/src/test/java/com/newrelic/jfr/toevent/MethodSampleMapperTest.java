package com.newrelic.jfr.toevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class MethodSampleMapperTest {

  @Test
  void testApply() {
    var threadName = "santiago";
    var threadState = "almost_asleep";
    var startTime = Instant.now();
    var expectedAttrs =
        new Attributes()
            .put("thread.name", threadName)
            .put("thread.state", threadState)
            .put(
                "stackTrace",
                "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":[]}");
    var expectedEvent = new Event("JfrMethodSample", expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var stack = mock(RecordedStackTrace.class);
    var sampledThread = mock(RecordedThread.class);

    when(stack.getFrames()).thenReturn(List.of());
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getStackTrace()).thenReturn(stack);
    when(event.getThread("sampledThread")).thenReturn(sampledThread);
    when(event.getString("state")).thenReturn(threadState);
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
