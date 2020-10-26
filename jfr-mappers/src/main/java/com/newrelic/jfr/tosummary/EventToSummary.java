/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.Constants;
import com.newrelic.telemetry.metrics.Summary;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

public interface EventToSummary extends Consumer<RecordedEvent>, Predicate<RecordedEvent> {

  /**
   * JFR event name (e.g. jdk.ObjectAllocationInNewTLAB)
   *
   * @return String representation of JFR event name
   */
  String getEventName();

  /**
   * Aggregates JFR Events into a collection based on thread or class name
   *
   * @param ev JFR RecordedEvent
   */
  void accept(RecordedEvent ev);

  /**
   * Test to see if this event is interesting to this summarizer
   *
   * @param event - event instance to see if we're interested
   * @return true if event is interesting, false otherwise
   */
  default boolean test(RecordedEvent event) {
    return event.getEventType().getName().startsWith(getEventName());
  }

  /**
   * Summarizes data on a collection of JFR Events
   *
   * @return List of Summary metrics for JFR Events
   */
  Stream<Summary> summarize();

  /** Clears the summary information */
  void reset();

  /**
   * Returns the Java version where particular JFR events were added.
   *
   * @return int indicating Java version that introduced the event type
   */
  default int since() {
    return Constants.JAVA_11;
  }
}
