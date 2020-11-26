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

public class JITCompilationFailureMapper implements EventToEvent {
  public static final String EVENT_NAME = "jdk.CompilationFailure";

  @Override
  public List<Event> apply(RecordedEvent event) {
    var timestamp = event.getStartTime().toEpochMilli();
    var duration = event.getDuration();
    var attr = new Attributes();
    attr.put("desc", MethodSupport.describeMethod(event.getValue("method")));
    attr.put("thread.name", event.getThread("eventThread").getJavaName());
    //    attr.put("duration", duration.toMillis());
    //    attr.put("succeeded", Workarounds.getSucceeded(event));

    return List.of(new Event("JfrCompilationFailure", attr, timestamp));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
