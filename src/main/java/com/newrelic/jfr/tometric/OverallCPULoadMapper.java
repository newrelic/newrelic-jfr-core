package com.newrelic.jfr.tometric;

import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

public class OverallCPULoadMapper implements EventToMetric {
    public static final String EVENT_NAME = "jdk.CPULoad";

    @Override
    public List<? extends Metric> apply(RecordedEvent ev) {
        var timestamp = ev.getStartTime().toEpochMilli();
        var attr = new Attributes();
        return List.of(
                new Gauge("jfr:CPULoad.jvmUser", ev.getDouble("jvmUser"), timestamp, attr),
                new Gauge("jfr:CPULoad.jvmSystem", ev.getDouble("jvmSystem"), timestamp, attr),
                new Gauge("jfr:CPULoad.machineTotal", ev.getDouble("machineTotal"), timestamp, attr)
        );
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
