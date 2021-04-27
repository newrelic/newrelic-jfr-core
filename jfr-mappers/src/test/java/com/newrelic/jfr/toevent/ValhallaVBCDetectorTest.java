package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.ValhallaVBCDetector.BOX_CLASS;
import static com.newrelic.jfr.toevent.ValhallaVBCDetector.EVENT_THREAD;
import static com.newrelic.jfr.toevent.ValhallaVBCDetector.JFR_VALHALLA_VBC_SYNC;
import static com.newrelic.jfr.toevent.ValhallaVBCDetector.STACK_TRACE;
import static com.newrelic.jfr.toevent.ValhallaVBCDetector.THREAD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

public class ValhallaVBCDetectorTest {
  private static final String JAVA_LANG_INTEGER = "java.lang.Integer";

  @Test
  void testApply() {
    var startTime = Instant.now();
    var threadName = "wonder";
    var expectedAttrs =
        new Attributes()
            .put(THREAD_NAME, threadName)
            .put(STACK_TRACE, MethodSupport.empty())
            .put(BOX_CLASS, JAVA_LANG_INTEGER);
    var expectedEvent = new Event(JFR_VALHALLA_VBC_SYNC, expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var eventThread = mock(RecordedThread.class);
    var stack = mock(RecordedStackTrace.class);
    when(stack.getFrames()).thenReturn(List.of());
    var clazz = mock(RecordedClass.class);
    when(clazz.getName()).thenReturn(JAVA_LANG_INTEGER);

    when(event.getClass(BOX_CLASS)).thenReturn(clazz);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getStackTrace()).thenReturn(stack);
    when(event.getThread(EVENT_THREAD)).thenReturn(eventThread);
    when(event.hasField(EVENT_THREAD)).thenReturn(true);
    when(event.hasField(BOX_CLASS)).thenReturn(true);

    when(eventThread.getJavaName()).thenReturn(threadName);

    var mapper = new ValhallaVBCDetector();

    var result = mapper.apply(event);

    assertEquals(expected, result);
  }
}
