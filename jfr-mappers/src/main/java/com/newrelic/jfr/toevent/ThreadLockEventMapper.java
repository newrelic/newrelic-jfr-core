/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public class ThreadLockEventMapper implements EventToEvent {
  public static final String EVENT_NAME = "jdk.JavaMonitorWait"; // jdk.JavaMonitorEnter

  @Override
  public List<Event> apply(RecordedEvent ev) {
    var duration = ev.getDuration();
    if (duration.toMillis() > 20) {
      var timestamp = ev.getStartTime().toEpochMilli();
      var attr = new Attributes();
      attr.put("duration", duration.toMillis());
      var eventThread = ev.getThread("eventThread");
      attr.put("thread.name", eventThread == null ? null : eventThread.getJavaName());
      var monitorClass = ev.getClass("monitorClass");
      attr.put("class", monitorClass == null ? null : monitorClass.getName());
      attr.put("stackTrace", MethodSupport.serialize(ev.getStackTrace()));
      return List.of(new Event("JfrJavaMonitorWait", attr, timestamp));
    }
    return List.of();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.of(1, SECONDS.toChronoUnit()));
  }
}
