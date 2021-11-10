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
import com.newrelic.jfr.toevent.GenericEventMapper;
import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.telemetry.Attributes;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class EventConverter {

  private static final Logger logger = LoggerFactory.getLogger(EventConverter.class.getName());

  private final Attributes commonAttributes;
  private final ToMetricRegistry toMetricRegistry;
  private final ToSummaryRegistry toSummaryRegistry;
  private final ToEventRegistry toEventRegistry;

  // AtomicInteger used as a counter, not for thread safety
  private final Map<String, AtomicInteger> eventCount = new HashMap<>();
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

    logger.debug("This conversion had {} events", eventCount.size());
    logger.debug("Detailed view of event counts: {}", eventCount);
    eventCount.clear();

    return batches;
  }

  private boolean isMetricEvent(RecordedEvent event) {
    return toMetricRegistry.all().anyMatch(m -> m.test(event));
  }

  private boolean isEventEvent(RecordedEvent event) {
    return toEventRegistry.all().anyMatch(m -> m.test(event));
  }

  private boolean isSummaryEvent(RecordedEvent event) {
    return toSummaryRegistry.all().anyMatch(m -> m.test(event));
  }

  private boolean isProfileEvent(RecordedEvent event) {
    return profilerRegistry.all().anyMatch(m -> m.test(event));
  }

  private void convertAndBuffer(BufferedTelemetry batches, RecordedEvent event) {
    String name = event.getEventType().getName();
    eventCount.computeIfAbsent(name, (key) -> new AtomicInteger()).incrementAndGet();
    try {

      if (isMetricEvent(event)) {
        // List of metric event names. Is name in list?

        toMetricRegistry
            .all()
            .filter(m -> m.test(event))
            .flatMap(m -> m.apply(event).stream())
            .forEach(batches::addMetric);

      } else if (isEventEvent(event)) {

        toEventRegistry
            .all()
            .filter(m -> m.test(event))
            .flatMap((m -> m.apply(event).stream()))
            .forEach(batches::addEvent);

      } else if (isSummaryEvent(event)) {
        toSummaryRegistry.all().filter(m -> m.test(event)).forEach(m -> m.accept(event));

      } else if (isProfileEvent(event)) {
          profilerRegistry.all().filter(m -> m.test(event)).forEach(m -> m.accept(event));

      } else {
        GenericEventMapper gem = new GenericEventMapper();
        gem.apply(event).forEach(batches::addEvent);

      }

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


}
