package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.profiler.MethodSupport;
import com.newrelic.jfr.toevent.MethodSampleMapper;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

import java.util.Collections;
import java.util.stream.Stream;

public class ProfileSummarizer implements EventToSummary {
    public static final String EVENT_NAME = "jdk.ExecutionSample";
    public static final String NATIVE_EVENT_NAME = "jdk.NativeMethodSample";

    private final String eventName;

    private ProfileSummarizer(final String eventName) {
        this.eventName = eventName;
    }

    public static ProfileSummarizer forExecutionSample() {
        return new ProfileSummarizer(EVENT_NAME);
    }

    public static ProfileSummarizer forNativeMethodSample() {
        return new ProfileSummarizer(NATIVE_EVENT_NAME);
    }


    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public void accept(RecordedEvent ev) {
        RecordedStackTrace trace = ev.getStackTrace();
        if (trace == null) {
            return;
        }

        long timestamp = ev.getStartTime().toEpochMilli();
        Attributes attr = new Attributes();
        RecordedThread sampledThread = ev.getThread("sampledThread");
        attr.put("thread.name", sampledThread == null ? null : sampledThread.getJavaName());
        attr.put("thread.state", ev.getString("state"));
        attr.put("stackTrace", MethodSupport.serialize(ev.getStackTrace()));


    }

    @Override
    public Stream<Summary> summarize() {
        return null;
    }

    @Override
    public void reset() {

    }
}
