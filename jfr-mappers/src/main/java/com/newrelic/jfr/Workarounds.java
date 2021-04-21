/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr;

import static com.newrelic.jfr.RecordedObjectValidators.*;

import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public class Workarounds {
  public static final String SIMPLE_CLASS_NAME = Workarounds.class.getSimpleName();
  public static final String EVENT_THREAD = "eventThread";
  public static final String SUCCEEDED = "succeeded";
  public static final String SUCCEDED_TYPO = "succeded";

  /**
   * There are cases where the event has the wrong type inside it for the thread, so calling {@link
   * RecordedEvent#getThread(String)} internally throws a {@link ClassCastException}. We work around
   * it here by just getting the raw value and checking the type.
   *
   * @param ev The event from which to carefully extract the thread
   * @return the thread name, or null if unable to extract it
   */
  public static Optional<String> getThreadName(RecordedEvent ev) {
    if (hasField(ev, EVENT_THREAD, SIMPLE_CLASS_NAME)) {
      Object thisField = ev.getValue(EVENT_THREAD);
      if (thisField instanceof RecordedThread) {
        return Optional.of(((RecordedThread) thisField).getJavaName());
      }
    }

    return Optional.empty();
  }

  /**
   * There is a typo in the field names in JFR. This unifies them in a way that is forward
   * compatible when the mistake is fixed. See
   * https://github.com/openjdk/jdk/blob/d74e4f22374b35ff5f84d320875b00313acdb14d/src/hotspot/share/jfr/metadata/metadata.xml#L494
   *
   * @param ev - The recorded event to check success for
   * @return true if the event succeeded/succeded, false otherwise
   */
  public static boolean getSucceeded(RecordedEvent ev) {
    if (hasField(ev, SUCCEEDED, SIMPLE_CLASS_NAME)) {
      return ev.getBoolean(SUCCEEDED);
    } else if (hasField(ev, SUCCEDED_TYPO, SIMPLE_CLASS_NAME)) {
      return ev.getBoolean(SUCCEDED_TYPO);
    }
    return false;
  }
}
