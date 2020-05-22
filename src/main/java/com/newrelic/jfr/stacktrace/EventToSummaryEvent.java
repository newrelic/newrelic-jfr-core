package com.newrelic.jfr.stacktrace;

import com.newrelic.jfr.Constants;
import com.newrelic.telemetry.events.Event;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

public interface EventToSummaryEvent extends Consumer<RecordedEvent> {

  /**
   * JFR event name (e.g. jdk.ObjectAllocationInNewTLAB)
   *
   * @return String representation of JFR event name
   */
  //    String getEventName();

  /**
   * Aggregates JFR Events into a collection based on thread or class name
   *
   * @param ev JFR RecordedEvent
   */
  void accept(RecordedEvent ev);

  /**
   * Summarizes data from a collection of JFR Events
   *
   * @return Stream of Events comprising the summarized
   */
  Stream<Event> summarizeAndReset();

  /**
   * Returns the Java version where particular JFR events were added.
   *
   * @return
   */
  default int since() {
    return Constants.JAVA_11;
  }
}
