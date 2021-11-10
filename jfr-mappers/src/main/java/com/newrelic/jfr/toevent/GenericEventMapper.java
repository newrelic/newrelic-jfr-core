package com.newrelic.jfr.toevent;

import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

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

    long timestamp = event.getStartTime().toEpochMilli();
    Duration duration = event.getDuration();
    Attributes attr = new Attributes();
    attr.put(DURATION, duration.toMillis());
    attr.put(SUCCEEDED, Workarounds.getSucceeded(event));
    return Collections.singletonList(
        new Event(JFR_PREFIX + event.getEventType().getName().substring(4), attr, timestamp));
  }
}
