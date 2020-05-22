package com.newrelic.jfr.toevent;

import com.newrelic.jfr.Constants;
import com.newrelic.telemetry.events.Event;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import jdk.jfr.consumer.RecordedEvent;

/**
 * Interface used to turn JFR RecordedEvent instances into lists of New Relic telemetry Events, with
 * a one-to-many mapping. The target Event is a type from the New Relic Telemetry SDK.
 */
public interface EventToEvent extends Function<RecordedEvent, List<Event>> {

  /**
   * JFR event name (e.g. jdk.ObjectAllocationInNewTLAB)
   *
   * @return String representation of JFR event name
   */
  String getEventName();

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
   * @return
   */
  default int since() {
    return Constants.JAVA_11;
  }
}
