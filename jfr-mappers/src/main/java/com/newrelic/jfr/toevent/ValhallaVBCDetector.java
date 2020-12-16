package com.newrelic.jfr.toevent;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

// jdk.SyncOnPrimitiveWrapper {
//        startTime = 17:16:25.584
//        boxClass = java.lang.Integer (classLoader = bootstrap)
//        eventThread = "main" (javaThreadId = 1)
//        stackTrace = [
//        com.company.Main.run() line: 21
//        com.company.Main.main(String[]) line: 10
//        ]
//        }
public class ValhallaVBCDetector implements EventToEvent {
  // Is this going to change to SyncOnValueBasedClass ?
  public static final String OLD_EVENT_NAME = "jdk.SyncOnPrimitiveWrapper";
  public static final String EVENT_NAME = "jdk.SyncOnValueBasedClass";

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public boolean test(RecordedEvent event) {
    var name = event.getEventType().getName();
    return name.equalsIgnoreCase(EVENT_NAME) || name.equalsIgnoreCase(OLD_EVENT_NAME);
  }

  @Override
  public List<Event> apply(RecordedEvent event) {
    var timestamp = event.getStartTime().toEpochMilli();
    var attr = new Attributes();
    var boxClass = event.getClass("boxClass");
    var eventThread = event.getThread("eventThread");
    if (boxClass != null) {
      attr.put("boxClass", boxClass.getName());
    }
    if (eventThread != null && eventThread.getJavaName() != null) {
      attr.put("thread.name", eventThread.getJavaName());
    }
    if (event.getStackTrace() != null) {
      attr.put("stackTrace", MethodSupport.serialize(event.getStackTrace()));
    }
    return List.of(new Event("JfrValhallaVBCSync", attr, timestamp));
  }
}
