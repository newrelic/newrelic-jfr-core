/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.RecordedObjectValidators.hasField;

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
  public static final String SIMPLE_CLASS_NAME = ThreadLockEventMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.JavaMonitorWait"; // jdk.JavaMonitorEnter
  public static final String MONITOR_CLASS = "monitorClass";
  public static final String CLASS = "class";
  public static final String THREAD_NAME = "thread.name";
  public static final String EVENT_THREAD = "eventThread";
  public static final String DURATION = "duration";
  public static final String STACK_TRACE = "stackTrace";
  public static final String JFR_JAVA_MONITOR_WAIT = "JfrJavaMonitorWait";

  @Override
  public List<Event> apply(RecordedEvent ev) {
    Duration duration = ev.getDuration();
    if (duration.toMillis() > 20) {
      long timestamp = ev.getStartTime().toEpochMilli();
      Attributes attr = new Attributes();
      if (hasField(ev, EVENT_THREAD, SIMPLE_CLASS_NAME)) {
        attr.put(THREAD_NAME, ev.getThread(EVENT_THREAD).getJavaName());
      }
      if (hasField(ev, MONITOR_CLASS, SIMPLE_CLASS_NAME)) {
        attr.put(CLASS, ev.getClass(MONITOR_CLASS).getName());
      }
      attr.put(DURATION, duration.toMillis());
      RecordedThread eventThread = null;
      if (hasField(ev, EVENT_THREAD, SIMPLE_CLASS_NAME)) {
        eventThread = ev.getThread(EVENT_THREAD);
      }
      attr.put(THREAD_NAME, eventThread == null ? null : eventThread.getJavaName());
      attr.put(STACK_TRACE, MethodSupport.serialize(ev.getStackTrace()));
      return Collections.singletonList(new Event(JFR_JAVA_MONITOR_WAIT, attr, timestamp));
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
