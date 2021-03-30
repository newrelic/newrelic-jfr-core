/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import com.newrelic.jfr.profiler.MethodSupport;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public class ThreadLockEventMapper implements EventToEvent {
  public static final String EVENT_NAME = "jdk.JavaMonitorWait"; // jdk.JavaMonitorEnter

  @Override
  public List<Event> apply(RecordedEvent ev) {
    Duration duration = ev.getDuration();
    if (duration.toMillis() > 20) {
      long timestamp = ev.getStartTime().toEpochMilli();
      Attributes attr = new Attributes();
      attr.put("thread.name", ev.getThread("eventThread").getJavaName());
      attr.put("class", ev.getClass("monitorClass").getName());
      attr.put("duration", duration.toMillis());
      RecordedThread eventThread = ev.getThread("eventThread");
      attr.put("thread.name", eventThread == null ? null : eventThread.getJavaName());
      attr.put("stackTrace", MethodSupport.serialize(ev.getStackTrace()));
      return Collections.singletonList(new Event("JfrJavaMonitorWait", attr, timestamp));
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
