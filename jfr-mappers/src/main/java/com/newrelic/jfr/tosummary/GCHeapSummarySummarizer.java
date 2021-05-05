/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.RecordedObjectValidators.*;
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
  public static final String SIMPLE_CLASS_NAME = GCHeapSummarySummarizer.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.GCHeapSummary";
  public static final String BEFORE = "Before GC";
  public static final String AFTER = "After GC";
  public static final String GC_ID = "gcId";
  public static final String WHEN = "when";

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
    String when = null;
    if (hasField(ev, WHEN, SIMPLE_CLASS_NAME)) {
      when = ev.getString(WHEN);
    }
    if (when != null) {
      if (!(when.equals(BEFORE) || when.equals(AFTER))) {
        return;
      }
    }
    count = count + 1;
    long gcId = 0;
    if (hasField(ev, GC_ID, SIMPLE_CLASS_NAME)) {
      gcId = ev.getLong(GC_ID);
    }
    RecordedEvent pair = awaitingPairs.get(gcId);
    if (pair == null) {
      awaitingPairs.put(gcId, ev);
    } else {
      awaitingPairs.remove(gcId);
      if (when != null && when.equals(BEFORE)) {
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
