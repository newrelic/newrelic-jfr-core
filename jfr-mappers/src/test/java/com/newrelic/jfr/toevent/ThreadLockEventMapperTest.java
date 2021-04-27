package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.ThreadLockEventMapper.CLASS;
import static com.newrelic.jfr.toevent.ThreadLockEventMapper.DURATION;
import static com.newrelic.jfr.toevent.ThreadLockEventMapper.EVENT_THREAD;
import static com.newrelic.jfr.toevent.ThreadLockEventMapper.JFR_JAVA_MONITOR_WAIT;
import static com.newrelic.jfr.toevent.ThreadLockEventMapper.MONITOR_CLASS;
import static com.newrelic.jfr.toevent.ThreadLockEventMapper.STACK_TRACE;
import static com.newrelic.jfr.toevent.ThreadLockEventMapper.THREAD_NAME;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class ThreadLockEventMapperTest {

  @Test
  void testApply() {
    var startTime = Instant.now();
    var threadName = "akita";
    var duration = Duration.of(21, MILLIS);
    var monitorClassName = "ooo";
    var expectedAttributes =
        new Attributes()
            .put(THREAD_NAME, threadName)
            .put(CLASS, monitorClassName)
            .put(DURATION, duration.toMillis())
            .put(STACK_TRACE, (String) null);
    var expectedEvent =
        new Event(JFR_JAVA_MONITOR_WAIT, expectedAttributes, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var monitorClass = mock(RecordedClass.class);
    var eventThread = mock(RecordedThread.class);

    when(event.getStartTime()).thenReturn(startTime);
    when(event.getThread(EVENT_THREAD)).thenReturn(eventThread);
    when(event.getDuration()).thenReturn(duration);
    when(event.getClass(MONITOR_CLASS)).thenReturn(monitorClass);
    when(eventThread.getJavaName()).thenReturn(threadName);
    when(monitorClass.getName()).thenReturn(monitorClassName);

    var mapper = new ThreadLockEventMapper();

    var result = mapper.apply(event);
    assertEquals(expected, result);
  }

  @Test
  public void testApplyButDurationTooShort() throws Exception {
    var duration = Duration.of(19, MILLIS);
    var expected = List.of();

    var event = mock(RecordedEvent.class);

    when(event.getDuration()).thenReturn(duration);

    var mapper = new ThreadLockEventMapper();

    var result = mapper.apply(event);
    assertEquals(expected, result);
  }
}
