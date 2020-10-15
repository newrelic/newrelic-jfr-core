package com.newrelic.jfr.tosummary;

import java.time.Duration;
import java.util.function.Supplier;
import jdk.jfr.consumer.RecordedEvent;

public class PairSummarizer extends BaseDurationSummarizer {

  public PairSummarizer(long startTimeMs, Supplier<Long> defaultClock, String duration) {
    super(startTimeMs, defaultClock, duration);
  }

  public void accept(RecordedEvent before, RecordedEvent after) {
    var duration =
        Duration.ofMillis(
            after.getStartTime().toEpochMilli() - before.getStartTime().toEpochMilli());
    endTimeMs = after.getStartTime().toEpochMilli();
    this.duration = this.duration.plus(duration);
    if (duration.compareTo(maxDuration) > 0) {
      maxDuration = duration;
    }
    if (duration.compareTo(minDuration) < 0) {
      minDuration = duration;
    }
  }
}
