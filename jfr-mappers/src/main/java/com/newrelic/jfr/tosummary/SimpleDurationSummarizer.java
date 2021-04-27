package com.newrelic.jfr.tosummary;

import java.time.Duration;
import java.util.function.Supplier;
import jdk.jfr.consumer.RecordedEvent;

public class SimpleDurationSummarizer extends BaseDurationSummarizer {
  public SimpleDurationSummarizer(long startTimeMs) {
    super(startTimeMs);
  }

  public SimpleDurationSummarizer(long startTimeMs, Supplier<Long> clock) {
    super(startTimeMs, clock);
  }

  public SimpleDurationSummarizer(long startTimeMs, Supplier<Long> clock, String durationName) {
    super(startTimeMs, clock, durationName);
  }

  private Duration getDuration(RecordedEvent ev) {
    if (durationName.isPresent() && ev.hasField(durationName.get())) {
      return ev.getDuration(durationName.get());
    }
    return ev.getDuration();
  }

  public void accept(RecordedEvent ev) {
    Duration duration = getDuration(ev);
    endTimeMs = ev.getStartTime().plus(duration).toEpochMilli();
    this.duration = this.duration.plus(duration);
    if (duration.compareTo(maxDuration) > 0) {
      maxDuration = duration;
    }
    if (duration.compareTo(minDuration) < 0) {
      minDuration = duration;
    }
  }
}
