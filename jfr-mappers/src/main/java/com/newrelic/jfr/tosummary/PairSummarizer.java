package com.newrelic.jfr.tosummary;

import java.util.function.Supplier;
import jdk.jfr.consumer.RecordedEvent;

public class PairSummarizer extends BaseDurationSummarizer {

  public PairSummarizer(long startTimeMs, Supplier<Long> defaultClock, String duration) {
    super(startTimeMs, defaultClock, duration);
  }

  public void accept(RecordedEvent before, RecordedEvent after) {}
}
