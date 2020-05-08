package com.newrelic.jfr.toevent;

import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.Attributes;
import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ThreadLockEventMapper implements EventToEvent {
    public static final String EVENT_NAME = "jdk.JavaMonitorWait"; // jdk.JavaMonitorEnter

    @Override
    public List<Event> apply(RecordedEvent ev) {
        var duration = ev.getDuration();
        if (duration.toMillis() > 20) {
            var timestamp = ev.getStartTime().toEpochMilli();
            var attr = new Attributes();
            attr.put("name", ev.getThread("eventThread").getJavaName());
            attr.put("class", ev.getClass("monitorClass").getName());
            attr.put("duration", duration.toMillis());

            return List.of(new Event("jfr:JavaMonitorWait", attr, timestamp));
        }
        return List.of();
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public Optional<Duration> getPollingDuration() {
        return Optional.of(Duration.of(1, SECONDS.toChronoUnit()));
    }

}
