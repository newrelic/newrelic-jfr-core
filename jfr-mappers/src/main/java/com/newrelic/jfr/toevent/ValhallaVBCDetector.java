package com.newrelic.jfr.toevent;

import com.newrelic.jfr.profiler.MethodSupport;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

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
    String name = event.getEventType().getName();
    return name.equalsIgnoreCase(EVENT_NAME) || name.equalsIgnoreCase(OLD_EVENT_NAME);
  }

  @Override
  public List<Event> apply(RecordedEvent event) {
    long timestamp = event.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    RecordedThread eventThread = event.getThread("eventThread");
    RecordedClass boxClass = event.getClass("boxClass");

    attr.put("thread.name", eventThread == null ? null : eventThread.getJavaName());
    attr.put("boxClass", boxClass == null ? null : boxClass.getName());
    attr.put("stackTrace", MethodSupport.serialize(event.getStackTrace()));
    return Collections.singletonList(new Event("JfrValhallaVBCSync", attr, timestamp));
  }
}
