package com.newrelic.jfr.tometric;

import com.newrelic.jfr.Constants;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import jdk.jfr.consumer.RecordedEvent;

/** Convenience/Tag interface for defining how JFR events should turn into metrics. */
public interface EventToMetric
    extends Function<RecordedEvent, List<? extends Metric>>, Predicate<RecordedEvent> {

  /**
   * JFR event name (e.g. jdk.ObjectAllocationInNewTLAB)
   *
   * @return String representation of JFR event name
   */
  String getEventName();

  /**
   * Test to see if this event is interesting to this mapper
   *
   * @param event
   * @return
   */
  default boolean test(RecordedEvent event) {
    return event.getEventType().getName().startsWith(getEventName());
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
   * @return
   */
  default int since() {
    return Constants.JAVA_11;
  }
}
