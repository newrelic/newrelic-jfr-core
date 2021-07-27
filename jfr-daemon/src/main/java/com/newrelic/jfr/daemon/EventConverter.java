/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import com.newrelic.jfr.ProfilerRegistry;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.profiler.EventToEventSummary;
import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.telemetry.Attributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventConverter {

  private static final Logger logger = LoggerFactory.getLogger(EventConverter.class.getName());

  private final Attributes commonAttributes;
  private final ToMetricRegistry toMetricRegistry;
  private final ToSummaryRegistry toSummaryRegistry;
  private final ToEventRegistry toEventRegistry;

  private final Map<String, IncrementableInteger> eventCount = new HashMap<>();
  private final ProfilerRegistry profilerRegistry;

  public EventConverter(Attributes commonAttributes, String pattern) {
    this(commonAttributes, new ThreadNameNormalizer(pattern));
  }

  private EventConverter(Attributes commonAttributes, ThreadNameNormalizer nameNormalizer) {
    this(
        commonAttributes,
        ToMetricRegistry.createDefault(),
        ToSummaryRegistry.create(nameNormalizer),
        ToEventRegistry.createDefault(),
        ProfilerRegistry.createDefault(nameNormalizer));
  }

  EventConverter(
      Attributes commonAttributes,
      ToMetricRegistry toMetricRegistry,
      ToSummaryRegistry toSummaryRegistry,
      ToEventRegistry toEventRegistry,
      ProfilerRegistry profilerRegistry) {
    this.commonAttributes = commonAttributes;
    this.toMetricRegistry = toMetricRegistry;
    this.toSummaryRegistry = toSummaryRegistry;
    this.toEventRegistry = toEventRegistry;
    this.profilerRegistry = profilerRegistry;
  }

  /**
   * Drain the events from the {@code buffer}, and convert them according to the configured metric,
   * event, and summary registries.
   *
   * @param buffer the buffer
   * @return a buffered telemetry containing the converted events
   */
  public BufferedTelemetry convert(RecordedEventBuffer buffer) {
    BufferedTelemetry batches = BufferedTelemetry.create(commonAttributes);

    buffer
        .drainToStream()
        .filter(Objects::nonNull)
        .forEach(recordedEvent -> convertAndBuffer(batches, recordedEvent));

    profilerRegistry.all().forEach(s -> s.summarize().forEach(batches::addEvent));
    profilerRegistry.all().forEach(EventToEventSummary::reset);

    toSummaryRegistry.all().forEach(s -> s.summarize().forEach(batches::addMetric));
    toSummaryRegistry.all().forEach(EventToSummary::reset);

    logger.info("This conversion had " + eventCount.size() + " events");
    logger.debug("Detailed view of event counts:  " + eventCount.toString());
    eventCount.clear();

    return batches;
  }

  private void convertAndBuffer(BufferedTelemetry batches, RecordedEvent event) {
    String name = event.getEventType().getName();
    eventCount.computeIfAbsent(name, (key) -> new IncrementableInteger()).inc();

    try {
      toMetricRegistry
          .all()
          .filter(m -> m.test(event))
          .flatMap(m -> m.apply(event).stream())
          .forEach(batches::addMetric);

      toEventRegistry
          .all()
          .filter(m -> m.test(event))
          .flatMap(m -> m.apply(event).stream())
          .forEach(batches::addEvent);

      toSummaryRegistry.all().filter(m -> m.test(event)).forEach(m -> m.accept(event));
      profilerRegistry.all().filter(m -> m.test(event)).forEach(m -> m.accept(event));

    } catch (Throwable e) {
      logger.error(
          "Dropping event "
              + event.getEventType().getName()
              + " "
              + event.getEventType().getDescription()
              + " due to error",
          e);
    }
  }

  /**
   * To be used as a counter when an Object is needed, as Integer is immutable.
   */
  private static class IncrementableInteger {
    private int value;

    public void inc() {
      value += 1;
    }

    public String toString() {
      return Integer.toString(value);
    }
  }
}
