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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates the duration of Garbage Collection JFR events */
public final class BasicGarbageCollectionSummarizer implements EventToSummary {

  public static final String EVENT_NAME = "jdk.GarbageCollection";

  private final SimpleDurationSummarizer minorGcDurationSummarizer;
  private final SimpleDurationSummarizer majorGcDurationSummarizer;
  private int minorGcCount = 0;
  private int majorGcCount = 0;
  private long startTimeMs;
  private long minorGcEndTimeMs = 0L;
  private long majorGcEndTimeMs = 0L;

  // TODO figure out exhaustive list of all minor/major GC names
  private static final Set<String> MINOR_GC_NAMES =
      new HashSet<String>() {
        {
          add("G1New");
          add("ParNew");
        }
      };

  private static final Set<String> MAJOR_GC_NAMES =
      new HashSet<String>() {
        {
          add("G1Old");
          add("ParallelOld");
        }
      };

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
    // TODO do we need a different start time for minor/major? Only end time?
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
      }
    }
  }

  // Metric types TODO Do we need all four or is the count included in duration sufficient?
  //    jfr.GarbageCollection.minorCount
  //    jfr.GarbageCollection.minorDuration
  //    jfr.GarbageCollection.majorCount
  //    jfr.GarbageCollection.majorDuration

  // Example JFR event
  //    jdk.GarbageCollection {
  //        startTime = 11:52:18.076
  //        duration = 0.502 ms
  //        gcId = 859
  //        name = "G1New"
  //        cause = "G1 Evacuation Pause"
  //        sumOfPauses = 0.502 ms
  //        longestPause = 0.502 ms
  //    }
  @Override
  public Stream<Summary> summarize() {
    Attributes attr = new Attributes();
    Summary minorGcDuration =
        new Summary(
            "jfr.GarbageCollection.minorDuration",
            minorGcCount, // TODO should count move to a jfr.GarbageCollection.minorCount metric?
            minorGcDurationSummarizer.getDurationMillis(),
            minorGcDurationSummarizer.getMinDurationMillis(),
            minorGcDurationSummarizer.getMaxDurationMillis(),
            startTimeMs,
            minorGcEndTimeMs,
            attr);

    Summary majorGcDuration =
        new Summary(
            "jfr.GarbageCollection.majorDuration",
            majorGcCount, // TODO should count move to a jfr.GarbageCollection.majorCount
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
