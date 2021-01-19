/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

// Only occurs at process startup
// jdk.InitialSystemProperty {
//        startTime = 10:37:04.315
//        key = "java.vm.specification.name"
//        value = "Java Virtual Machine Specification"
// }
public class JVMSystemPropertyMapper implements EventToEvent {
  public static final String EVENT_NAME = "jdk.InitialSystemProperty";

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public List<Event> apply(RecordedEvent event) {
    long timestamp = event.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    attr.put("jvmProperty", event.getString("key"));
    attr.put("jvmPropertyValue", event.getString("value"));

    return Collections.singletonList(new Event("JfrJVMInformation", attr, timestamp));
  }
}
