package com.newrelic.jfr.toevent;

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
    expectedAttrs.put("jvmProperty", key);
    expectedAttrs.put("jvmPropertyValue", value);

    var expectedEvent = new Event("JfrJVMInformation", expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var mapper = new JVMSystemPropertyMapper(new AttributeValueSplitter());

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getString("key")).thenReturn(key);
    when(event.getString("value")).thenReturn(value);

    var result = mapper.apply(event);

    assertEquals(expected, result);
  }
}
