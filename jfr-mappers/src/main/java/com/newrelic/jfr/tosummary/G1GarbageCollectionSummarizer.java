/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.tosummary.DurationSummarizer.DEFAULT_CLOCK;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Instant;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates the duration of G1 Garbage Collection JFR events */
public final class G1GarbageCollectionSummarizer implements EventToSummary {

  public static final String EVENT_NAME = "jdk.G1GarbageCollection";

  private final DurationSummarizer summarizer;
  private int count = 0;
  private long startTimeMs;
  private long endTimeMs = 0L;

  public G1GarbageCollectionSummarizer() {
    this(Instant.now().toEpochMilli());
  }

  public G1GarbageCollectionSummarizer(long startTimeMs) {
    this(startTimeMs, new DurationSummarizer(startTimeMs, DEFAULT_CLOCK, "duration"));
  }

  public G1GarbageCollectionSummarizer(long startTimeMs, DurationSummarizer summarizer) {
    this.startTimeMs = startTimeMs;
    this.summarizer = summarizer;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    endTimeMs = ev.getStartTime().toEpochMilli();
    count++;
    summarizer.accept(ev);
  }

  @Override
  public Stream<Summary> summarizeAndReset() {
    var attr = new Attributes();
    var out =
        new Summary(
            "jfr.G1GarbageCollection.duration",
            count,
            summarizer.getDurationMillis(),
            summarizer.getMinDurationMillis(),
            summarizer.getMaxDurationMillis(),
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
    summarizer.reset();
  }
}
