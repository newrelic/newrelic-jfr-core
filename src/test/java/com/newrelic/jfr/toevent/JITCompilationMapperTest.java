package com.newrelic.jfr.toevent;

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

class JITCompilationMapperTest {

  @Test
  void testApply() {
    var startTime = Instant.now();
    var threadName = "wonder";
    var monitorClassName = "java101";
    var duration = Duration.ofSeconds(14);
    var expectedAttrs =
        new Attributes()
            .put("thread.name", threadName)
            .put("duration", duration.toMillis())
            .put("class", monitorClassName)
            .put("succeeded", true);
    var expectedEvent = new Event("jfr:Compilation", expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var eventThread = mock(RecordedThread.class);
    var monitorClass = mock(RecordedClass.class);

    when(event.getStartTime()).thenReturn(startTime);
    when(event.getDuration()).thenReturn(duration);
    when(event.getThread("eventThread")).thenReturn(eventThread);
    when(event.getClass("monitorClass")).thenReturn(monitorClass);
    when(event.hasField("succeeded")).thenReturn(true);
    when(event.getBoolean("succeeded")).thenReturn(true);
    when(monitorClass.getName()).thenReturn(monitorClassName);
    when(eventThread.getJavaName()).thenReturn(threadName);

    var mapper = new JITCompilationMapper();

    var result = mapper.apply(event);

    assertEquals(expected, result);
  }
}
