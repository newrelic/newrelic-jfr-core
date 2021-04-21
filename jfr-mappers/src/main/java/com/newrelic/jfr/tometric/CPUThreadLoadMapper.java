/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.RecordedObjectValidators.*;

import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

// jdk.ThreadCPULoad {
//        startTime = 10:37:24.287
//        user = 0.00%
//        system = 0.00%
//        eventThread = "C1 CompilerThread0" (javaThreadId = 8)
//        }
public class CPUThreadLoadMapper implements EventToMetric {
  private static final String SIMPLE_CLASS_NAME = CPUThreadLoadMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.ThreadCPULoad";
  public static final String USER = "user";
  public static final String SYSTEM = "system";
  public static final String THREAD_NAME = "thread.name";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    Optional<String> possibleThreadName = Workarounds.getThreadName(ev);
    if (possibleThreadName.isPresent()) {
      String threadName = possibleThreadName.get();
      long timestamp = ev.getStartTime().toEpochMilli();
      Attributes attr = new Attributes().put(THREAD_NAME, threadName);
      double userGaugeValue = 0;
      if (hasField(ev, USER, SIMPLE_CLASS_NAME)) {
        userGaugeValue = ev.getDouble(USER);
      }
      double systemGaugeValue = 0;
      if (hasField(ev, SYSTEM, SIMPLE_CLASS_NAME)) {
        systemGaugeValue = ev.getDouble(SYSTEM);
      }
      // Do we need to throttle these events somehow? Or just send everything?
      return Arrays.asList(
          new Gauge("jfr.ThreadCPULoad.user", userGaugeValue, timestamp, attr),
          new Gauge("jfr.ThreadCPULoad.system", systemGaugeValue, timestamp, attr));
    }
    return Collections.emptyList();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.of(1, ChronoUnit.SECONDS));
  }
}
