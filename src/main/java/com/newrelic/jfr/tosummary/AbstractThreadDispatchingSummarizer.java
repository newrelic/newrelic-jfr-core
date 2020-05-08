package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import jdk.jfr.consumer.RecordedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractThreadDispatchingSummarizer implements EventToSummary {
    protected final Map<String, EventToSummary> perThread = new HashMap<>();

    @Override
    public Stream<Summary> summarizeAndReset() {
        return perThread
                .values()
                .stream()
                .flatMap(EventToSummary::summarizeAndReset);
    }

    public abstract String getEventName();

    public abstract void accept(RecordedEvent ev);
}
