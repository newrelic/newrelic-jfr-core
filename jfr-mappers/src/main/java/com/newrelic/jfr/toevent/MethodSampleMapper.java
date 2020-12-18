/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

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
  public static final String EVENT_NAME = "jdk.ExecutionSample";
  public static final String NATIVE_EVENT_NAME = "jdk.NativeMethodSample";

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
    var trace = ev.getStackTrace();
    if (trace == null) {
      return List.of();
    }

    var timestamp = ev.getStartTime().toEpochMilli();
    var attr = new Attributes();
    var sampledThread = ev.getThread("sampledThread");
    attr.put("thread.name", sampledThread == null ? null : sampledThread.getJavaName());
    attr.put("thread.state", ev.getString("state"));
    attr.put("stackTrace", MethodSupport.serialize(ev.getStackTrace()));

    return List.of(new Event("JfrMethodSample", attr, timestamp));
  }

  @Override
  public String getEventName() {
    return eventName;
  }
}
