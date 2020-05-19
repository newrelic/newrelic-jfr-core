package com.newrelic.jfr.tosummary;

import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.time.Instant;

public class DurationSummarizer {

    private long startTimeMs;
    private long endTimeMs;
    private Duration duration = Duration.ofNanos(0L);
    private Duration minDuration = Duration.ofNanos(Long.MAX_VALUE);
    private Duration maxDuration = Duration.ofNanos(0L);

    public DurationSummarizer(long startTimeMs) {
        this.startTimeMs = startTimeMs;
        this.endTimeMs = this.startTimeMs;
    }

    public void accept(RecordedEvent ev) {
        Duration duration = ev.getDuration();
        endTimeMs = ev.getStartTime().plus(duration).toEpochMilli();
        this.duration = this.duration.plus(duration);
        if (duration.compareTo(maxDuration) > 0) {
            maxDuration = duration;
        }
        if (duration.compareTo(minDuration) < 0) {
            minDuration = duration;
        }
    }

    public void reset() {
        startTimeMs = Instant.now().toEpochMilli();
        endTimeMs = 0L;
        duration = Duration.ofNanos(0L);
        minDuration = Duration.ofNanos(Long.MAX_VALUE);
        maxDuration = Duration.ofNanos(0L);
    }


    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public double getDurationMillis() {
        return duration.toMillis();
    }

    public double getMinDurationMillis() {
        return minDuration.toMillis();
    }

    public double getMaxDurationMillis() {
        return maxDuration.toMillis();
    }
}
