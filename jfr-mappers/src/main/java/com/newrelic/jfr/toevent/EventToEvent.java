/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.toevent;

import com.newrelic.jfr.Constants;
import com.newrelic.telemetry.events.Event;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import jdk.jfr.consumer.RecordedEvent;

/**
 * Interface used to turn JFR RecordedEvent instances into lists of New Relic telemetry Events, with
 * a one-to-many mapping. The target Event is a type from the New Relic Telemetry SDK.
 */
public interface EventToEvent
    extends Function<RecordedEvent, List<Event>>, Predicate<RecordedEvent> {

  /**
   * JFR event name (e.g. jdk.ObjectAllocationInNewTLAB)
   *
   * @return String representation of JFR event name
   */
  String getEventName();

  /**
   * Test to see if this event is interesting to this mapper
   *
   * @param event - event instance to see if we're interested
   * @return true if event is interesting, false otherwise
   */
  default boolean test(RecordedEvent event) {
    return event.getEventType().getName().equalsIgnoreCase(getEventName());
  }

  /**
   * Optionally returns a polling duration for JFR events, if present
   *
   * @return {@link Optional} of {@link Duration} representing polling duration; empty {@link
   *     Optional} if no polling
   */
  default Optional<Duration> getPollingDuration() {
    return Optional.empty();
  }

  /**
   * Returns the Java version where particular JFR events were added.
   *
   * @return int indicating Java version that introduced the event type
   */
  default int since() {
    return Constants.JAVA_11;
  }
}
