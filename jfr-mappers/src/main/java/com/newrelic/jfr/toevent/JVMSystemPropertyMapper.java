/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.RecordedObjectValidators.hasField;

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
  public static final String SIMPLE_CLASS_NAME = JVMSystemPropertyMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.InitialSystemProperty";
  public static final String JVM_PROPERTY = "jvmProperty";
  public static final String KEY = "key";
  public static final String JVM_PROPERTY_VALUE = "jvmPropertyValue";
  public static final String VALUE = "value";
  public static final String JFR_JVM_INFORMATION = "JfrJVMInformation";

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public List<Event> apply(RecordedEvent event) {
    long timestamp = event.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();

    if (hasField(event, KEY, SIMPLE_CLASS_NAME)) {
      attr.put(JVM_PROPERTY, event.getString(KEY));
    }
    if (hasField(event, VALUE, SIMPLE_CLASS_NAME)) {
      attr.put(JVM_PROPERTY_VALUE, event.getString(VALUE));
    }
    return Collections.singletonList(new Event(JFR_JVM_INFORMATION, attr, timestamp));
  }
}
