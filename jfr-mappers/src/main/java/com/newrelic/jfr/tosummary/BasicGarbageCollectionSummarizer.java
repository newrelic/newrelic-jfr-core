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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class aggregates the duration of Garbage Collection JFR events */
public final class BasicGarbageCollectionSummarizer implements EventToSummary {
  private static final Logger logger =
      LoggerFactory.getLogger(BasicGarbageCollectionSummarizer.class);

  public static final String EVENT_NAME = "jdk.GarbageCollection";

  private final SimpleDurationSummarizer minorGcDurationSummarizer;
  private final SimpleDurationSummarizer majorGcDurationSummarizer;
  private int minorGcCount = 0;
  private int majorGcCount = 0;
  private long startTimeMs;
  private long minorGcEndTimeMs = 0L;
  private long majorGcEndTimeMs = 0L;

  private static final Set<String> MINOR_GC_NAMES =
      Collections.unmodifiableSet(
          new HashSet<String>() {
            {
              add("DefNew");
              add("G1New");
              add("ParallelScavenge");
              add("ParNew");
              add("PSMarkSweep");
            }
          });

  private static final Set<String> MAJOR_GC_NAMES =
      Collections.unmodifiableSet(
          new HashSet<String>() {
            {
              add("ConcurrentMarkSweep");
              add("G1Full");
              add("G1Old");
              add("ParallelOld");
              add("SerialOld");
            }
          });

  public BasicGarbageCollectionSummarizer() {
    this(Instant.now().toEpochMilli());
  }

  public BasicGarbageCollectionSummarizer(long startTimeMs) {
    this(
        startTimeMs,
        new SimpleDurationSummarizer(startTimeMs, DEFAULT_CLOCK, "duration"),
        new SimpleDurationSummarizer(startTimeMs, DEFAULT_CLOCK, "duration"));
  }

  public BasicGarbageCollectionSummarizer(
      long startTimeMs,
      SimpleDurationSummarizer minorGcDurationSummarizer,
      SimpleDurationSummarizer majorGcDurationSummarizer) {
    this.startTimeMs = startTimeMs;
    this.minorGcDurationSummarizer = minorGcDurationSummarizer;
    this.majorGcDurationSummarizer = majorGcDurationSummarizer;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    final String name = ev.getValue("name");
    if (name != null) {
      if (MINOR_GC_NAMES.contains(name)) {
        minorGcEndTimeMs = ev.getStartTime().toEpochMilli();
        minorGcDurationSummarizer.accept(ev);
        minorGcCount++;
      } else if (MAJOR_GC_NAMES.contains(name)) {
        majorGcEndTimeMs = ev.getStartTime().toEpochMilli();
        majorGcDurationSummarizer.accept(ev);
        majorGcCount++;
      } else
        // Ignore events with GC name: GCNameEndSentinel, N/A, Shenandoah, Z or anything unexpected
        logger.warn("Ignoring unsupported " + EVENT_NAME + " event: " + name);
    }
  }

  @Override
  public Stream<Summary> summarize() {
    Attributes attr = new Attributes();
    Summary minorGcDuration =
        new Summary(
            "jfr.GarbageCollection.minorDuration",
            minorGcCount,
            minorGcDurationSummarizer.getDurationMillis(),
            minorGcDurationSummarizer.getMinDurationMillis(),
            minorGcDurationSummarizer.getMaxDurationMillis(),
            startTimeMs,
            minorGcEndTimeMs,
            attr);

    Summary majorGcDuration =
        new Summary(
            "jfr.GarbageCollection.majorDuration",
            majorGcCount,
            majorGcDurationSummarizer.getDurationMillis(),
            majorGcDurationSummarizer.getMinDurationMillis(),
            majorGcDurationSummarizer.getMaxDurationMillis(),
            startTimeMs,
            majorGcEndTimeMs,
            attr);
    return Stream.of(minorGcDuration, majorGcDuration);
  }

  public void reset() {
    startTimeMs = Instant.now().toEpochMilli();
    minorGcEndTimeMs = 0L;
    majorGcEndTimeMs = 0L;
    minorGcCount = 0;
    majorGcCount = 0;
    minorGcDurationSummarizer.reset();
    majorGcDurationSummarizer.reset();
  }
}
