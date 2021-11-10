/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Instant;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

// jdk.ObjectCount {
//        startTime = 10:52:17.997
//        gcId = 28
//        objectClass = com.newrelic.agent.deps.org.objectweb.asm.tree.FieldInsnNode (classLoader =
// com.newrelic.bootstrap.BootstrapAgent$JVMAgentClassLoader)
//        count = 10998
//        totalSize = 515.5 kB
//        }
/**
 * Summarizes the JFR event jdk.ObjectCount. Records the count and size for the class in each event.
 * Uses the count for the count in the summary and total size for all other fields in the summary.
 * This class is not thread safe.
 */
public class ObjectCountSummarizer implements EventToSummary {

  private static final String JFR_EVENT_NAME = "jdk.ObjectCount";
  private static final String OBJECT_CLASS = "objectClass";
  private static final String COUNT_FIELD = "count";
  private static final String TOTAL_SIZE_FIELD = "totalSize";
  private static final String METRIC_NAME = "jfr.ObjectCount.allocation";
  private static final String CLASS_ATTR = "class";

  // Deques to be used as stacks, so whenever adding a new element, the previous one can be easily
  // retrieved
  private final Map<String, Deque<CountSize>> perClass = new HashMap<>();
  private long startTimeMs;

  @Override
  public String getEventName() {
    return JFR_EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    String className = ev.getClass(OBJECT_CLASS).getName();

    Deque<CountSize> entries = perClass.computeIfAbsent(className, key -> new LinkedList<>());
    CountSize previousEntry = entries.peekFirst();
    entries.addFirst(new CountSize(ev, calculateNextStartTime(previousEntry)));
  }

  private long calculateNextStartTime(CountSize previousEntry) {
    if (previousEntry != null) {
      return previousEntry.endTimeMs;
    }
    return startTimeMs;
  }

  @Override
  public Stream<Summary> summarize() {
    return perClass.entrySet().stream().flatMap(this::summarizeClass);
  }

  private Stream<Summary> summarizeClass(Map.Entry<String, Deque<CountSize>> entry) {
    String className = entry.getKey();
    Attributes atts = new Attributes().put(CLASS_ATTR, className);
    return entry.getValue().stream().map(countSize -> newSummary(countSize, atts));
  }

  private Summary newSummary(CountSize countSize, Attributes atts) {
    return new Summary(
        METRIC_NAME,
        countSize.count,
        countSize.size,
        countSize.size,
        countSize.size,
        countSize.startTimeMs,
        countSize.endTimeMs,
        atts);
  }

  @Override
  public void reset() {
    perClass.clear();
    startTimeMs = Instant.now().toEpochMilli();
  }

  private static class CountSize {
    private final int count;
    private final long size;
    private final long startTimeMs;
    private final long endTimeMs;

    public CountSize(RecordedEvent ev, long startTimeMs) {
      this.count = (int) ev.getLong(COUNT_FIELD);
      this.size = ev.getLong(TOTAL_SIZE_FIELD);
      this.startTimeMs = startTimeMs;
      this.endTimeMs = ev.getStartTime().toEpochMilli();
    }
  }
}
