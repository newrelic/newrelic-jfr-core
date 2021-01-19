/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

// jdk.Compilation {
//        startTime = 16:04:14.403
//        duration = LGTM - but I'm not a reviewer :( ms
//        method = org.apache.kafka.clients.Metadata.update(Cluster, Set, long)
//        compileId = 30333
//        compileLevel = 4
//        succeded = true
//        isOsr = false
//        codeSize = 36.1 kB
//        inlinedBytes = 2.9 kB
//        eventThread = "C2 CompilerThread0" (javaThreadId = 5)
//        }
public class JITCompilationMapper implements EventToEvent {
  public static final String EVENT_NAME = "jdk.Compilation";

  @Override
  public List<Event> apply(RecordedEvent event) {
    long timestamp = event.getStartTime().toEpochMilli();
    Duration duration = event.getDuration();
    Attributes attr = new Attributes();
    attr.put("desc", MethodSupport.describeMethod(event.getValue("method")));
    attr.put("thread.name", event.getThread("eventThread").getJavaName());
    attr.put("duration", duration.toMillis());
    attr.put("succeeded", Workarounds.getSucceeded(event));

    return Collections.singletonList(new Event("JfrCompilation", attr, timestamp));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
