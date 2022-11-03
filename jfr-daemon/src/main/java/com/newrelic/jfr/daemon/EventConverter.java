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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventConverter {

  private static final Logger logger = LoggerFactory.getLogger(EventConverter.class.getName());

  private final Attributes commonAttributes;
  private final ToMetricRegistry toMetricRegistry;
  private final ToSummaryRegistry toSummaryRegistry;
  private final ToEventRegistry toEventRegistry;

  // AtomicInteger used as a counter, not for thread safety
  private final Map<String, AtomicInteger> eventCount = new HashMap<>();
  private final ProfilerRegistry profilerRegistry;
  private BufferedTelemetry batches;

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
    this.batches = BufferedTelemetry.create(commonAttributes);
  }

  /**
   * Drain the events from the {@code buffer}, and convert them according to the configured metric,
   * event, and summary registries.
   *
   * @return a buffered telemetry containing the converted events
   */
  public BufferedTelemetry harvest() {
    BufferedTelemetry currentBatch = batches;
    batches = BufferedTelemetry.create(commonAttributes);

    profilerRegistry.all().forEach(s -> s.summarize().forEach(currentBatch::addEvent));
    profilerRegistry.all().forEach(EventToEventSummary::reset);

    toSummaryRegistry.all().forEach(s -> s.summarize().forEach(currentBatch::addMetric));
    toSummaryRegistry.all().forEach(EventToSummary::reset);

    eventCount.clear();

    return currentBatch;
  }

  public void convertAndBuffer(RecordedEvent event) {
    String name = event.getEventType().getName();
//    System.out.println("Converting and buffering: " + name);
    eventCount.computeIfAbsent(name, (key) -> new AtomicInteger()).incrementAndGet();

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

  public Collection<String> getEventNames() {
    Collection<String> eventNames = new HashSet<>();
    eventNames.addAll(toMetricRegistry.getEventNames());
    eventNames.addAll(toSummaryRegistry.getEventNames());
    eventNames.addAll(toEventRegistry.getEventNames());
    eventNames.addAll(profilerRegistry.getEventNames());
    return eventNames;
  }
}
