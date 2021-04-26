/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.RecordedObjectValidators.hasField;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

// Need to handle both jdk.ExecutionSample and jdk.NativeMethodSample...

// jdk.NativeMethodSample {
//        startTime = 10:37:26.131
//        sampledThread = "JFR Periodic Tasks" (javaThreadId = 12)
//        state = "STATE_IN_OBJECT_WAIT_TIMED"
//        stackTrace = [
//        java.lang.Object.wait(long)
//        jdk.jfr.internal.PlatformRecorder.takeNap(long) line: 449
//        jdk.jfr.internal.PlatformRecorder.periodicTask() line: 442
//        jdk.jfr.internal.PlatformRecorder.lambda$startDiskMonitor$1() line: 387
//        jdk.jfr.internal.PlatformRecorder$$Lambda$50.1866850137.run()
//        ...
//        ]
//        }
public class MethodSampleMapper implements EventToEvent {
  public static final String SIMPLE_CLASS_NAME = MethodSampleMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.ExecutionSample";
  public static final String NATIVE_EVENT_NAME = "jdk.NativeMethodSample";
  public static final String THREAD_STATE = "thread.state";
  public static final String STATE = "state";
  public static final String THREAD_NAME = "thread.name";
  public static final String SAMPLED_THREAD = "sampledThread";
  public static final String STACK_TRACE = "stackTrace";
  public static final String JFR_METHOD_SAMPLE = "JfrMethodSample";

  private final String eventName;

  private MethodSampleMapper(final String eventName) {
    this.eventName = eventName;
  }

  public static MethodSampleMapper forExecutionSample() {
    return new MethodSampleMapper(EVENT_NAME);
  }

  public static MethodSampleMapper forNativeMethodSample() {
    return new MethodSampleMapper(NATIVE_EVENT_NAME);
  }

  @Override
  public List<Event> apply(RecordedEvent ev) {
    RecordedStackTrace trace = ev.getStackTrace();
    if (trace == null) {
      return Collections.emptyList();
    }

    long timestamp = ev.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    RecordedThread sampledThread = null;
    if (hasField(ev, SAMPLED_THREAD, SIMPLE_CLASS_NAME)) {
      sampledThread = ev.getThread(SAMPLED_THREAD);
    }
    attr.put(THREAD_NAME, sampledThread == null ? null : sampledThread.getJavaName());
    if (hasField(ev, STATE, SIMPLE_CLASS_NAME)) {
      attr.put(THREAD_STATE, ev.getString(STATE));
    }
    attr.put(STACK_TRACE, MethodSupport.serialize(ev.getStackTrace()));
    return Collections.singletonList(new Event(JFR_METHOD_SAMPLE, attr, timestamp));
  }

  @Override
  public String getEventName() {
    return eventName;
  }
}
