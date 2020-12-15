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

public abstract class BaseDurationSummarizer {

  public static final Supplier<Long> DEFAULT_CLOCK = () -> Instant.now().toEpochMilli();
  private final Supplier<Long> clock;
  protected final Optional<String> durationName;
  private long startTimeMs;
  protected long endTimeMs;
  protected Duration duration = Duration.ofNanos(0L);
  protected Duration minDuration = Duration.ofNanos(Long.MAX_VALUE);
  protected Duration maxDuration = Duration.ofNanos(Long.MIN_VALUE);

  public BaseDurationSummarizer(long startTimeMs) {
    this(startTimeMs, DEFAULT_CLOCK);
  }

  public BaseDurationSummarizer(long startTimeMs, Supplier<Long> clock) {
    this(startTimeMs, clock, null);
  }

  public BaseDurationSummarizer(long startTimeMs, Supplier<Long> clock, String durationName) {
    this.startTimeMs = startTimeMs;
    this.endTimeMs = this.startTimeMs;
    this.clock = clock;
    this.durationName = Optional.ofNullable(durationName);
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
