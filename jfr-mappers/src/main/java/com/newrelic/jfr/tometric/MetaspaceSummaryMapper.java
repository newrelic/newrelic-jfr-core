/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tometric;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

public class MetaspaceSummaryMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.MetaspaceSummary";
  public static final String NR_METRIC_PREFIX = "jfr.MetaspaceSummary.";
  public static final String METASPACE = "metaspace";
  public static final String DATA_SPACE = "dataSpace";
  public static final String CLASS_SPACE = "classSpace";
  public static final String WHEN = "when";
  public static final String COMMITTED = "committed";
  public static final String USED = "used";
  public static final String RESERVED = "reserved";
  public static final String DOT_DELIMITER = ".";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    RecordedObject metaspace = null;
    if (ev.hasField(METASPACE)) {
      metaspace = ev.getValue(METASPACE);
    }
    RecordedObject dataSpace = null;
    if (ev.hasField(DATA_SPACE)) {
      dataSpace = ev.getValue(DATA_SPACE);
    }
    RecordedObject classSpace = null;
    if (ev.hasField(CLASS_SPACE)) {
      classSpace = ev.getValue(CLASS_SPACE);
    }
    Attributes attr = new Attributes();
    if (ev.hasField(WHEN)) {
      attr.put(WHEN, ev.getString(WHEN));
    }
    List<Metric> metrics = new ArrayList<>(9);
    metrics.addAll(generateMetric(METASPACE, metaspace, attr, timestamp));
    metrics.addAll(generateMetric(DATA_SPACE, dataSpace, attr, timestamp));
    metrics.addAll(generateMetric(CLASS_SPACE, classSpace, attr, timestamp));
    return metrics;
  }

  private List<? extends Metric> generateMetric(
      String name, RecordedObject recordedObject, Attributes attr, long timestamp) {
    if (recordedObject != null) {
      double committedGaugeValue = 0;
      if (recordedObject.hasField(COMMITTED)) {
        committedGaugeValue = recordedObject.getDouble(COMMITTED);
      }
      double usedGaugeValue = 0;
      if (recordedObject.hasField(USED)) {
        usedGaugeValue = recordedObject.getDouble(USED);
      }
      double reservedGaugeValue = 0;
      if (recordedObject.hasField(RESERVED)) {
        reservedGaugeValue = recordedObject.getDouble(RESERVED);
      }

      return Arrays.asList(
          new Gauge(
              NR_METRIC_PREFIX + name + DOT_DELIMITER + COMMITTED,
              committedGaugeValue,
              timestamp,
              attr),
          new Gauge(
              NR_METRIC_PREFIX + name + DOT_DELIMITER + USED, usedGaugeValue, timestamp, attr),
          new Gauge(
              NR_METRIC_PREFIX + name + DOT_DELIMITER + RESERVED,
              reservedGaugeValue,
              timestamp,
              attr));
    }
    return Collections.emptyList();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
