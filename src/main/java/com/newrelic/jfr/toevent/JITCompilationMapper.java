package com.newrelic.jfr.toevent;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedMethod;

// jdk.Compilation {
//        startTime = 16:04:14.403
//        duration = LGTM - but I'm not a reviewer :( ms
//        method = org.apache.kafka.clients.Metadata.update(Cluster, Set, long)
//        compileId = 30333
//        compileLevel = 4
//        succeded = true
//        isOsr = false
//        codeSize = 36.1 kB
//        inlinedBytes = 2.9 kB
//        eventThread = "C2 CompilerThread0" (javaThreadId = 5)
//        }
public class JITCompilationMapper implements EventToEvent {
  public static final String EVENT_NAME = "jdk.Compilation";

  @Override
  public List<Event> apply(RecordedEvent event) {
    var timestamp = event.getStartTime().toEpochMilli();
    var duration = event.getDuration();
    var attr = new Attributes();
    attr.put("desc", MethodSupport.describeMethod((RecordedMethod) event.getValue("method")));
    attr.put("thread.name", event.getThread("eventThread").getJavaName());
    attr.put("duration", duration.toMillis());
    attr.put("succeeded", Workarounds.getSucceeded(event));

    return List.of(new Event("jfr:Compilation", attr, timestamp));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
