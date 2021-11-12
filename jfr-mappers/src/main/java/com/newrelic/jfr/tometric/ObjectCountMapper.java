/*
 *
 *  * Copyright 2021 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tometric;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import java.util.Arrays;
import java.util.List;
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
 * Records the count and size for the class in each jdk.ObjectCount event in the metrics:
 * jfr.ObjectCount.count and fjr.ObjectCount.totalSize.
 */
public class ObjectCountMapper implements EventToMetric {

  private static final String EVENT_NAME = "jdk.ObjectCount";

  // visible for testing
  static final String METRIC_NAME_PREFIX = "jfr.ObjectCount.";
  static final String COUNT_METRIC_NAME = METRIC_NAME_PREFIX + "count";
  static final String TOTAL_SIZE_METRIC_NAME = METRIC_NAME_PREFIX + "totalSize";
  static final String OBJECT_CLASS = "objectClass";
  static final String COUNT_FIELD = "count";
  static final String TOTAL_SIZE_FIELD = "totalSize";
  static final String CLASS_ATTR = "class";

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public List<Gauge> apply(RecordedEvent ev) {
    String className = ev.getClass(OBJECT_CLASS).getName();
    Attributes atts = new Attributes().put(CLASS_ATTR, className);

    int count = (int) ev.getLong(COUNT_FIELD);
    long totalSize = ev.getLong(TOTAL_SIZE_FIELD);
    long startTime = ev.getStartTime().toEpochMilli();
    return Arrays.asList(
        new Gauge(COUNT_METRIC_NAME, count, startTime, atts),
        new Gauge(TOTAL_SIZE_METRIC_NAME, totalSize, startTime, atts));
  }
}
