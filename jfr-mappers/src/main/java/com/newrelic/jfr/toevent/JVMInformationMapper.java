/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.RecordedObjectValidators.*;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.Collections;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;

// Only occurs at process startup
// jdk.JVMInformation {
//        startTime = 10:37:04.314
//        jvmName = "OpenJDK 64-Bit Server VM"
//        jvmVersion = "OpenJDK 64-Bit Server VM (11.0.4+11) for bsd-amd64 JRE (11.0.4+11), built on
// Aug
//        5 2019 02:57:07 by "jenkins" with gcc 4.2.1 Compatible Apple LLVM 7.0.2 (clang-700.1.81)"
//        jvmArguments = "-XX:+PrintCompilation
// -XX:StartFlightRecording:disk=true,filename=opt-java.jfr
//        ,maxage=12h,settings=profile"
//        jvmFlags = N/A
//        javaArguments = "optjava.StringHash"
//        jvmStartTime = 10:37:03.849
//        pid = 13612
//        }
public class JVMInformationMapper implements EventToEvent {
  public static final String SIMPLE_CLASS_NAME = JVMInformationMapper.class.getSimpleName();
  public static final String EVENT_NAME = "jdk.JVMInformation";
  public static final String JVM_START_TIME = "jvmStartTime";
  public static final String JVM_ARGUMENTS = "jvmArguments";
  public static final String JVM_VERSION = "jvmVersion";

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public List<Event> apply(RecordedEvent event) {

    long timestamp = event.getStartTime().toEpochMilli();
    Attributes attr = new Attributes();
    if (hasField(event, JVM_START_TIME, SIMPLE_CLASS_NAME)) {
      attr.put(JVM_START_TIME, event.getInstant(JVM_START_TIME).toEpochMilli());
    }
    if (hasField(event, JVM_ARGUMENTS, SIMPLE_CLASS_NAME)) {
      attr.put(JVM_ARGUMENTS, event.getString(JVM_ARGUMENTS));
    }
    if (hasField(event, JVM_VERSION, SIMPLE_CLASS_NAME)) {
      attr.put(JVM_VERSION, event.getString(JVM_VERSION));
    }

    return Collections.singletonList(new Event("JfrJVMInformation", attr, timestamp));
  }
}
