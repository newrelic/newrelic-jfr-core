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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class aggregates the duration of Garbage Collection JFR events */
public final class BasicGarbageCollectionSummarizer implements EventToSummary {
  public static final String EVENT_NAME = "jdk.GarbageCollection";
  public static final String NAME = "name";
  public static final String JFR_GARBAGE_COLLECTION_MINOR_DURATION =
      "jfr.GarbageCollection.minorDuration";
  public static final String JFR_GARBAGE_COLLECTION_MAJOR_DURATION =
      "jfr.GarbageCollection.majorDuration";
  public static final String DURATION = "duration";
  public static final String DEF_NEW = "DefNew";
  public static final String G1_NEW = "G1New";
  public static final String PARALLEL_SCAVENGE = "ParallelScavenge";
  public static final String PAR_NEW = "ParNew";
  public static final String PS_MARK_SWEEP = "PSMarkSweep";
  public static final String CONCURRENT_MARK_SWEEP = "ConcurrentMarkSweep";
  public static final String G1_FULL = "G1Full";
  public static final String G1_OLD = "G1Old";
  public static final String PARALLEL_OLD = "ParallelOld";
  public static final String SERIAL_OLD = "SerialOld";

  private static final Logger logger =
      LoggerFactory.getLogger(BasicGarbageCollectionSummarizer.class);
  private final SimpleDurationSummarizer minorGcDurationSummarizer;
  private final SimpleDurationSummarizer majorGcDurationSummarizer;
  private final AtomicInteger minorGcCount = new AtomicInteger(0);
  private final AtomicInteger majorGcCount = new AtomicInteger(0);
  private long startTimeMs;
  private long minorGcEndTimeMs = 0L;
  private long majorGcEndTimeMs = 0L;

  private static final Set<String> MINOR_GC_NAMES =
      Collections.unmodifiableSet(
          new HashSet<String>() {
            {
              add(DEF_NEW);
              add(G1_NEW);
              add(PARALLEL_SCAVENGE);
              add(PAR_NEW);
              add(PS_MARK_SWEEP);
            }
          });

  private static final Set<String> MAJOR_GC_NAMES =
      Collections.unmodifiableSet(
          new HashSet<String>() {
            {
              add(CONCURRENT_MARK_SWEEP);
              add(G1_FULL);
              add(G1_OLD);
              add(PARALLEL_OLD);
              add(SERIAL_OLD);
            }
          });

  public BasicGarbageCollectionSummarizer() {
    this(Instant.now().toEpochMilli());
  }

  public BasicGarbageCollectionSummarizer(long startTimeMs) {
    this(
        startTimeMs,
        new SimpleDurationSummarizer(startTimeMs, DEFAULT_CLOCK, DURATION),
        new SimpleDurationSummarizer(startTimeMs, DEFAULT_CLOCK, DURATION));
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
    String name = null;
    if (ev.hasField(NAME)) {
      name = ev.getValue(NAME);
    }
    if (name != null) {
      if (MINOR_GC_NAMES.contains(name)) {
        minorGcEndTimeMs = ev.getStartTime().toEpochMilli();
        minorGcDurationSummarizer.accept(ev);
        minorGcCount.incrementAndGet();
      } else if (MAJOR_GC_NAMES.contains(name)) {
        majorGcEndTimeMs = ev.getStartTime().toEpochMilli();
        majorGcDurationSummarizer.accept(ev);
        majorGcCount.incrementAndGet();
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
            JFR_GARBAGE_COLLECTION_MINOR_DURATION,
            minorGcCount.get(),
            minorGcDurationSummarizer.getDurationMillis(),
            minorGcDurationSummarizer.getMinDurationMillis(),
            minorGcDurationSummarizer.getMaxDurationMillis(),
            startTimeMs,
            minorGcEndTimeMs,
            attr);

    Summary majorGcDuration =
        new Summary(
            JFR_GARBAGE_COLLECTION_MAJOR_DURATION,
            majorGcCount.get(),
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
    minorGcCount.set(0);
    majorGcCount.set(0);
    minorGcDurationSummarizer.reset();
    majorGcDurationSummarizer.reset();
  }
}
