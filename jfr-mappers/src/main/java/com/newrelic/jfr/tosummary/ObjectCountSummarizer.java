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
import java.util.HashMap;
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
 */
public class ObjectCountSummarizer implements EventToSummary {

  private final Map<String, CountSize> perClass = new HashMap<>();
  private long endTimeMs;
  private static final String JFR_EVENT_NAME = "jdk.ObjectCount";
  private static final String OBJECT_CLASS = "objectClass";
  private static final String COUNT_FIELD = "count";
  private static final String TOTAL_SIZE_FIELD = "totalSize";
  private static final String METRIC_NAME = "jfr.ObjectCount.allocation";
  private static final String CLASS_ATTR = "class";

  @Override
  public String getEventName() {
    return JFR_EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    String className = ev.getClass(OBJECT_CLASS).getName();
    // there is a bug here in case there are 2 events for the same class in the same harvest cycle.
    // only the 2nd datapoint will remain
    perClass.put(className, new CountSize(ev));
  }

  @Override
  public Stream<Summary> summarize() {
    endTimeMs = Instant.now().toEpochMilli();
    return perClass.entrySet().stream().map(this::summarizeClass);
  }

  private Summary summarizeClass(Map.Entry<String, CountSize> entry) {
    String className = entry.getKey();
    Attributes atts = new Attributes().put(CLASS_ATTR, className);
    CountSize countSize = entry.getValue();
    return new Summary(
        METRIC_NAME,
        countSize.count,
        countSize.size,
        countSize.size,
        countSize.size,
        // not entirely sure how time should work here. It will have to be refactored to fix
        // multiple events in a single harvest cycle
        countSize.startTime,
        endTimeMs,
        atts);
  }

  @Override
  public void reset() {
    perClass.clear();
  }

  private static class CountSize {
    private final int count;
    private final long size;
    private final long startTime;

    public CountSize(RecordedEvent ev) {
      this.count = (int) ev.getLong(COUNT_FIELD);
      this.size = ev.getLong(TOTAL_SIZE_FIELD);
      this.startTime = ev.getStartTime().toEpochMilli();
    }
  }
}
