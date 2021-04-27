package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.JFR_JVM_INFORMATION;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.JVM_PROPERTY;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.JVM_PROPERTY_VALUE;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.KEY;
import static com.newrelic.jfr.toevent.JVMSystemPropertyMapper.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class JVMSystemPropertyMapperTest {

  @Test
  void testApply() {
    var key = "key1";
    var value = "value1";
    var startTime = Instant.now();

    var expectedAttrs = new Attributes();
    expectedAttrs.put(JVM_PROPERTY, key);
    expectedAttrs.put(JVM_PROPERTY_VALUE, value);

    var expectedEvent = new Event(JFR_JVM_INFORMATION, expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var mapper = new JVMSystemPropertyMapper(new AttributeValueSplitter());

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getString(KEY)).thenReturn(key);
    when(event.getString(VALUE)).thenReturn(value);
    when(event.hasField(KEY)).thenReturn(true);
    when(event.hasField(VALUE)).thenReturn(true);

    var result = mapper.apply(event);

    assertEquals(expected, result);
  }
}
