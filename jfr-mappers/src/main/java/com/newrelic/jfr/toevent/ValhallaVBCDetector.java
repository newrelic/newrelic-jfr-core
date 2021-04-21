package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.RecordedObjectValidators.hasField;

import com.newrelic.jfr.MethodSupport;
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
  public static final String SIMPLE_CLASS_NAME = ValhallaVBCDetector.class.getSimpleName();
  // Is this going to change to SyncOnValueBasedClass ?
  public static final String OLD_EVENT_NAME = "jdk.SyncOnPrimitiveWrapper";
  public static final String EVENT_NAME = "jdk.SyncOnValueBasedClass";
  public static final String BOX_CLASS = "boxClass";
  public static final String EVENT_THREAD = "eventThread";
  public static final String THREAD_NAME = "thread.name";
  public static final String STACK_TRACE = "stackTrace";

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
    RecordedThread eventThread = null;
    if (hasField(event, EVENT_THREAD, SIMPLE_CLASS_NAME)) {
      eventThread = event.getThread(EVENT_THREAD);
    }
    RecordedClass boxClass = null;
    if (hasField(event, BOX_CLASS, SIMPLE_CLASS_NAME)) {
      boxClass = event.getClass(BOX_CLASS);
    }
    attr.put(THREAD_NAME, eventThread == null ? null : eventThread.getJavaName());
    attr.put(BOX_CLASS, boxClass == null ? null : boxClass.getName());
    attr.put(STACK_TRACE, MethodSupport.serialize(event.getStackTrace()));
    return Collections.singletonList(new Event("JfrValhallaVBCSync", attr, timestamp));
  }
}
