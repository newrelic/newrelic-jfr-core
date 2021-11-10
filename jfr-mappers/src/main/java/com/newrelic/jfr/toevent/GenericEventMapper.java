package com.newrelic.jfr.toevent;

import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class GenericEventMapper implements EventToEvent{

//    public static final String SIMPLE_CLASS_NAME = JITCompilationMapper.class.getSimpleName();
    private final String EVENT_NAME = "genericEvent";
//    public static final String METHOD = "method";
//    public static final String DESC = "desc";
    public static final String DURATION = "duration";
    public static final String SUCCEEDED = "succeeded";
    public static final String EVENT_THREAD = "eventThread";
    public static final String THREAD_NAME = "thread.name";

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
//        RecordedThread threadId = null;
//        if (hasField(event, EVENT_THREAD, SIMPLE_CLASS_NAME)) {
//            threadId = event.getThread(EVENT_THREAD);
//        }
//        attr.put(THREAD_NAME, threadId == null ? null : threadId.getJavaName());
        return Collections.singletonList(new Event("Jfr" + event.getEventType().getName().substring(4), attr, timestamp));
    }
}
