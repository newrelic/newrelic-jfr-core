package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.Constants;
import com.newrelic.telemetry.metrics.Summary;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

public interface EventToSummary extends Consumer<RecordedEvent> {

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
   * Summarizes data on a collection of JFR Events
   *
   * @return List of Summary metrics for JFR Events
   */
  Stream<Summary> summarizeAndReset();

  /**
   * Returns the Java version where particular JFR events were added.
   *
   * @return
   */
  default int since() {
    return Constants.JAVA_11;
  }
}
