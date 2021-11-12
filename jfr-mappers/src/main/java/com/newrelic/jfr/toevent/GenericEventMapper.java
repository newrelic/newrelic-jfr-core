package com.newrelic.jfr.toevent;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.Collections;
import java.util.List;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;

public class GenericEventMapper implements EventToEvent {
  //EVENT_NAME is not used to name the event
  //there is a test for EventToEvent that requires this field
  //Other Mappers use this field to match the mapper to the event. It's not needed here.
  //We name the event based off of whatever Jfr Event is configured and see as it passes through
  //the Event Converters.
  private final String EVENT_NAME = "genericEvent";
  public static final String DURATION = "duration";
  public static final String SUCCEEDED = "succeeded";
  public static final String EVENT_THREAD = "eventThread";
  public static final String THREAD_NAME = "thread.name";
  private static final String JFR_PREFIX = "Jfr";

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public List<Event> apply(RecordedEvent event) {
    Attributes attr = new Attributes();
    event.getFields().forEach(field -> createAttributes(event, field, attr));
    long timestamp = event.getStartTime().toEpochMilli();
    return Collections.singletonList(
        new Event(JFR_PREFIX + event.getEventType().getName().substring(4), attr, timestamp));
  }

  private void createAttributes(RecordedEvent event, ValueDescriptor field, Attributes attr) {
    String fieldName = field.getName();
    if (!fieldName.equals("startTime") && event.getValue(fieldName) != null) {
      int typeId = Math.toIntExact(field.getTypeId());
      switch (typeId) {
        case 4:
          attr.put(fieldName, event.getBoolean(fieldName));
          break;
        case 5:
          attr.put(fieldName, event.getString(fieldName));
          break;
        case 6:
          attr.put(fieldName, event.getFloat(fieldName));
          break;
        case 7:
          attr.put(fieldName, event.getDouble(fieldName));
          break;
        case 8:
          attr.put(fieldName, event.getByte(fieldName));
          break;
        case 9:
          attr.put(fieldName, event.getShort(fieldName));
          break;
        case 10:
          attr.put(fieldName, event.getInt(fieldName));
          break;
        case 11:
          attr.put(fieldName, event.getLong(fieldName));
          break;
        default:
          attr.put(fieldName, event.getValue(fieldName).toString());
      }
    }
  }
}
