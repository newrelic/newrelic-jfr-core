package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates the duration of G1 Garbage Collection JFR events */
public final class G1GarbageCollectionSummarizer implements EventToSummary {

  public static final String EVENT_NAME = "jdk.G1GarbageCollection";

  private int count = 0;
  private Duration sum = Duration.ofNanos(0L);
  private Duration min = Duration.ofNanos(Long.MAX_VALUE);
  private Duration max = Duration.ofNanos(0L);
  private long startTimeMs;
  private long endTimeMs = 0L;

  public G1GarbageCollectionSummarizer() {
    startTimeMs = Instant.now().toEpochMilli();
  }

  public G1GarbageCollectionSummarizer(long startTimeMs) {
    this.startTimeMs = startTimeMs;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    endTimeMs = ev.getStartTime().toEpochMilli();
    count++;
    var duration = ev.getDuration("duration");
    sum = sum.plus(duration);

    if (duration.compareTo(max) > 0) {
      max = duration;
    }
    if (duration.compareTo(min) < 0) {
      min = duration;
    }
  }

  @Override
  public Stream<Summary> summarizeAndReset() {
    var attr = new Attributes();
    var out =
        new Summary(
            "jfr:G1GarbageCollection.duration",
            count,
            sum.toMillis(),
            min.toMillis(),
            max.toMillis(),
            startTimeMs,
            endTimeMs,
            attr);
    reset();
    return Stream.of(out);
  }

  public void reset() {
    startTimeMs = Instant.now().toEpochMilli();
    endTimeMs = 0L;
    count = 0;
    sum = Duration.ofNanos(0L);
    min = Duration.ofNanos(Long.MAX_VALUE);
    max = Duration.ofNanos(0L);
  }
}
