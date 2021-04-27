package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.JVMInformationMapper.JFR_JVM_INFORMATION;
import static com.newrelic.jfr.toevent.JVMInformationMapper.JVM_ARGUMENTS;
import static com.newrelic.jfr.toevent.JVMInformationMapper.JVM_START_TIME;
import static com.newrelic.jfr.toevent.JVMInformationMapper.JVM_VERSION;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class JVMInformationMapperTest {

  @Test
  void testApply() {
    var jvmArgs = "-Xmx9001m";
    var jvmVersion = "11.0.1";
    var startTime = Instant.now();
    var eventTime = Instant.now().plus(12, MILLIS);
    var expectedAttributes =
        new Attributes()
            .put(JVM_ARGUMENTS, jvmArgs)
            .put(JVM_START_TIME, startTime.toEpochMilli())
            .put(JVM_VERSION, jvmVersion);
    var expectedEvent =
        new Event(JFR_JVM_INFORMATION, expectedAttributes, eventTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);

    when(event.getStartTime()).thenReturn(eventTime);
    when(event.getString(JVM_ARGUMENTS)).thenReturn(jvmArgs);
    when(event.getInstant(JVM_START_TIME)).thenReturn(startTime);
    when(event.getString(JVM_VERSION)).thenReturn(jvmVersion);
    when(event.hasField(JVM_ARGUMENTS)).thenReturn(true);
    when(event.hasField(JVM_START_TIME)).thenReturn(true);
    when(event.hasField(JVM_VERSION)).thenReturn(true);

    var mapper = new JVMInformationMapper();

    var result = mapper.apply(event);

    assertEquals(expected, result);
  }
}
