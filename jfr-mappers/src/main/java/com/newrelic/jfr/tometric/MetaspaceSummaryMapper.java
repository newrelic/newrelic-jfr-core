/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.RecordedObjectValidators.*;

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
  private static final String SIMPLE_CLASS_NAME = MetaspaceSummaryMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.MetaspaceSummary";
  static final String NR_METRIC_PREFIX = "jfr.MetaspaceSummary.";
  static final String METASPACE = "metaspace";
  static final String DATA_SPACE = "dataSpace";
  static final String CLASS_SPACE = "classSpace";
  static final String WHEN = "when";
  static final String COMMITTED = "committed";
  static final String USED = "used";
  static final String RESERVED = "reserved";
  static final String DOT_DELIMITER = ".";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    RecordedObject metaspace = null;
    if (hasField(ev, METASPACE, SIMPLE_CLASS_NAME)) {
      metaspace = ev.getValue(METASPACE);
    }
    RecordedObject dataSpace = null;
    if (hasField(ev, DATA_SPACE, SIMPLE_CLASS_NAME)) {
      dataSpace = ev.getValue(DATA_SPACE);
    }
    RecordedObject classSpace = null;
    if (hasField(ev, CLASS_SPACE, SIMPLE_CLASS_NAME)) {
      classSpace = ev.getValue(CLASS_SPACE);
    }
    Attributes attr = new Attributes();
    if (hasField(ev, WHEN, SIMPLE_CLASS_NAME)) {
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
    if (!isRecordedObjectNull(recordedObject, SIMPLE_CLASS_NAME)) {
      double committedGaugeValue = 0;
      if (hasField(recordedObject, COMMITTED, SIMPLE_CLASS_NAME)) {
        committedGaugeValue = recordedObject.getDouble(COMMITTED);
      }
      double usedGaugeValue = 0;
      if (hasField(recordedObject, USED, SIMPLE_CLASS_NAME)) {
        usedGaugeValue = recordedObject.getDouble(USED);
      }
      double reservedGaugeValue = 0;
      if (hasField(recordedObject, RESERVED, SIMPLE_CLASS_NAME)) {
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
