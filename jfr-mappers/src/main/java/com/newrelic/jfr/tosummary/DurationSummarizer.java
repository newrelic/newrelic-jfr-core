/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import jdk.jfr.consumer.RecordedEvent;

public class DurationSummarizer {

  public static final Supplier<Long> DEFAULT_CLOCK = () -> Instant.now().toEpochMilli();
  private final Supplier<Long> clock;
  private final Optional<String> durationName;
  private long startTimeMs;
  private long endTimeMs;
  private Duration duration = Duration.ofNanos(0L);
  private Duration minDuration = Duration.ofNanos(Long.MAX_VALUE);
  private Duration maxDuration = Duration.ofNanos(Long.MIN_VALUE);

  public DurationSummarizer(long startTimeMs) {
    this(startTimeMs, DEFAULT_CLOCK);
  }

  public DurationSummarizer(long startTimeMs, Supplier<Long> clock) {
    this(startTimeMs, clock, null);
  }

  public DurationSummarizer(long startTimeMs, Supplier<Long> clock, String durationName) {
    this.startTimeMs = startTimeMs;
    this.endTimeMs = this.startTimeMs;
    this.clock = clock;
    this.durationName = Optional.ofNullable(durationName);
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

  private Duration getDuration(RecordedEvent ev) {
    return durationName.map(ev::getDuration).orElse(ev.getDuration());
  }

  public void reset() {
    startTimeMs = clock.get();
    endTimeMs = 0L;
    duration = Duration.ofNanos(0L);
    minDuration = Duration.ofNanos(Long.MAX_VALUE);
    maxDuration = Duration.ofNanos(Long.MIN_VALUE);
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
