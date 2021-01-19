/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.tosummary.BaseDurationSummarizer.DEFAULT_CLOCK;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/**
 * This class aggregates the duration of GCHeapSummary JFR events. For GC purposes they come in
 * pairs.
 */
public final class GCHeapSummarySummarizer implements EventToSummary {

  public static final String EVENT_NAME = "jdk.GCHeapSummary";
  private static final String BEFORE = "Before GC";
  private static final String AFTER = "After GC";

  private final Map<Long, RecordedEvent> awaitingPairs = new HashMap<>();

  private final PairSummarizer summarizer;
  private int count = 0;
  private long startTimeMs;
  private long endTimeMs = 0L;

  public GCHeapSummarySummarizer() {
    this(Instant.now().toEpochMilli());
  }

  public GCHeapSummarySummarizer(long startTimeMs) {
    this(startTimeMs, new PairSummarizer(startTimeMs, DEFAULT_CLOCK, "duration"));
  }

  public GCHeapSummarySummarizer(long startTimeMs, PairSummarizer summarizer) {
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
    String when = ev.getString("when");
    if (!(when.equals(BEFORE) || when.equals(AFTER))) {
      return;
    }

    count = count + 1;
    long gcId = ev.getLong("gcId");
    RecordedEvent pair = awaitingPairs.get(gcId);
    if (pair == null) {
      awaitingPairs.put(gcId, ev);
    } else {
      awaitingPairs.remove(gcId);
      if (when.equals(BEFORE)) {
        summarizer.accept(ev, pair);
      } else { //  i.e. when.equals(AFTER)
        summarizer.accept(pair, ev);
      }
    }
  }

  @Override
  public Stream<Summary> summarize() {
    Attributes attr = new Attributes();
    Summary out =
        new Summary(
            "jfr.GarbageCollection.duration",
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

  @Override
  public void reset() {
    startTimeMs = Instant.now().toEpochMilli();
    endTimeMs = 0L;
    count = 0;
    summarizer.reset();
  }
}
