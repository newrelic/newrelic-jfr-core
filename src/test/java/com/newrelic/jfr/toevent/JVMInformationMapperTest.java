package com.newrelic.jfr.toevent;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JVMInformationMapperTest {

    @Test
    void testApply() {
        var jvmArgs = "-Xmx9001m";
        var jvmVersion = "11.0.1";
        var startTime = Instant.now();
        var eventTime = Instant.now().plus(12, MILLIS);
        var expectedAttributes = new Attributes()
                .put("jvmArguments", jvmArgs)
                .put("jvmStartTime", startTime.toEpochMilli())
                .put("jvmVersion", jvmVersion);
        var expectedEvent = new Event("jfr:JVMInformation", expectedAttributes, eventTime.toEpochMilli());
        var expected = List.of(expectedEvent);

        var event = mock(RecordedEvent.class);

        when(event.getStartTime()).thenReturn(eventTime);
        when(event.getString("jvmArguments")).thenReturn(jvmArgs);
        when(event.getInstant("jvmStartTime")).thenReturn(startTime);
        when(event.getString("jvmVersion")).thenReturn(jvmVersion);

        var mapper = new JVMInformationMapper();

        var result = mapper.apply(event);

        assertEquals(expected, result);
    }

}