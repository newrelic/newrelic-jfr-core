/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Instant;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates all TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationInNewTLABSummarizer implements EventToSummary {
  private final String threadName;
  private final LongSummarizer summarizer;
  private long startTimeMs;
  private long endTimeMs = 0L;
  public static final String JFR_OBJECT_ALLOCATION_IN_NEW_TLAB_ALLOCATION =
      "jfr.ObjectAllocationInNewTLAB.allocation";
  public static final String THREAD_NAME = "thread.name";
  public static final String TLAB_SIZE = "tlabSize";

  public PerThreadObjectAllocationInNewTLABSummarizer(String threadName, long startTimeMs) {
    this(threadName, startTimeMs, new LongSummarizer(TLAB_SIZE));
  }

  public PerThreadObjectAllocationInNewTLABSummarizer(
      String threadName, long startTimeMs, LongSummarizer summarizer) {
    this.threadName = threadName;
    this.startTimeMs = startTimeMs;
    this.summarizer = summarizer;
  }

  @Override
  public String getEventName() {
    return ObjectAllocationInNewTLABSummarizer.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    endTimeMs = ev.getStartTime().toEpochMilli();
    summarizer.accept(ev);
    // Probably too high a cardinality
    // ev.getClass("objectClass").getName();
  }

  @Override
  public Stream<Summary> summarize() {
    Attributes attr = new Attributes().put(THREAD_NAME, threadName);
    Summary out =
        new Summary(
            JFR_OBJECT_ALLOCATION_IN_NEW_TLAB_ALLOCATION,
            summarizer.getCount(),
            summarizer.getSum(),
            summarizer.getMin(),
            summarizer.getMax(),
            startTimeMs,
            endTimeMs,
            attr);
    return Stream.of(out);
  }

  public void reset() {
    startTimeMs = Instant.now().toEpochMilli();
    endTimeMs = 0L;
    summarizer.reset();
  }
}
