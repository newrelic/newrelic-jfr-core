package com.newrelic.jfr.stacktrace;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import jdk.jfr.consumer.RecordedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// jdk.ExecutionSample
public class StackTraceSummarizer implements EventToSummaryEvent {

    private int count = 0;
    private long startTimeMs;
    private long endTimeMs = 0L;

    private List<Map<String, Integer>> framesByDescCount = new ArrayList<>();

    public StackTraceSummarizer(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    @Override
    public void accept(RecordedEvent ev) {
        var trace = ev.getStackTrace();
        if (trace != null) {
            endTimeMs = ev.getStartTime().toEpochMilli();
            count++;

            String b64 = StackTraceBlob.encodeB64(trace);

            var frames = trace.getFrames();
            for (int i = 0; i < frames.size(); i++) {
                var frame = frames.get(i);
                var method = frame.getMethod();
                var methodDesc = method.getType() +"."+ method.getName() + method.getDescriptor();
                if (framesByDescCount.get(i) == null) {
                    framesByDescCount.set(i, new HashMap<>());
                }
                var countsByDesc = framesByDescCount.get(i);
                int newCount = countsByDesc.getOrDefault(methodDesc, 1);
                countsByDesc.put(methodDesc, newCount);
            }

        }
    }

    @Override
    public Stream<Event> summarizeAndReset() {
        var attr = new Attributes();


        var out = new Event("jfr:StackTracePrototype", attr, endTimeMs);
        reset();
        return Stream.of(out);
    }

    public void reset() {
        startTimeMs = Instant.now().toEpochMilli();
        endTimeMs = 0L;
    }
}
