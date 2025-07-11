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
import java.util.UUID;
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
    this.commonAttributes = validateAttributes(commonAttributes);
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

  /**
   * This is gross. If the entity.guid AND service.instance.id keys are missing, we need to assign a
   * random UUID to the service.instance.id key. The presence of entity.guid indicates we're running
   * embedded in the agent. If it's not there, we're running stand alone and the service.instance.id
   * is required.
   *
   * @param attributes the Attributes instance to update
   * @return the updated Attribute instance
   */
  private Attributes validateAttributes(Attributes attributes) {
    Map<String, Object> attributesAsMap = attributes.asMap();
    if (!attributesAsMap.containsKey(AttributeNames.ENTITY_GUID)
        && !attributesAsMap.containsKey(AttributeNames.SERVICE_INSTANCE_ID)) {
      attributes.put(AttributeNames.SERVICE_INSTANCE_ID, UUID.randomUUID().toString());
    }

    return attributes;
  }

  private void convertAndBuffer(BufferedTelemetry batches, RecordedEvent event) {
    String name = event.getEventType().getName();
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
}
