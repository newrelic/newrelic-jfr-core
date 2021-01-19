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
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;

public class MetaspaceSummaryMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.MetaspaceSummary";
  static final String NR_METRIC_PREFIX = "jfr.MetaspaceSummary.";
  static final String METASPACE_KEY = "metaspace";
  static final String DATA_SPACE_KEY = "dataSpace";
  static final String CLASS_SPACE_KEY = "classSpace";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    long timestamp = ev.getStartTime().toEpochMilli();
    RecordedObject metaspace = ev.getValue(METASPACE_KEY);
    RecordedObject dataSpace = ev.getValue(DATA_SPACE_KEY);
    RecordedObject classSpace = ev.getValue(CLASS_SPACE_KEY);

    Attributes attr = new Attributes().put("when", ev.getString("when"));

    List<Metric> metrics = new ArrayList<>(9);
    metrics.addAll(generateMetric(METASPACE_KEY, metaspace, attr, timestamp));
    metrics.addAll(generateMetric(DATA_SPACE_KEY, dataSpace, attr, timestamp));
    metrics.addAll(generateMetric(CLASS_SPACE_KEY, classSpace, attr, timestamp));

    return metrics;
  }

  private List<? extends Metric> generateMetric(
      String name, RecordedObject recordedObject, Attributes attr, long timestamp) {
    return Arrays.asList(
        new Gauge(
            NR_METRIC_PREFIX + name + ".committed",
            recordedObject.getDouble("committed"),
            timestamp,
            attr),
        new Gauge(
            NR_METRIC_PREFIX + name + ".used", recordedObject.getDouble("used"), timestamp, attr),
        new Gauge(
            NR_METRIC_PREFIX + name + ".reserved",
            recordedObject.getDouble("reserved"),
            timestamp,
            attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
