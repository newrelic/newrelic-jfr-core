/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.telemetry.Attributes;
import java.util.HashMap;
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

  private final Map<String, AtomicInteger> eventCount = new HashMap<>();

  public EventConverter(Builder builder) {
    this.commonAttributes = builder.commonAttributes;
    this.toMetricRegistry = builder.toMetricRegistry;
    this.toSummaryRegistry = builder.toSummaryRegistry;
    this.toEventRegistry = builder.toEventRegistry;
  }

  public BufferedTelemetry convert(RecordedEventBuffer buffer) {
    var batches = BufferedTelemetry.create(commonAttributes);

    buffer
        .drainToStream()
        .filter(Objects::nonNull)
        .forEach(recordedEvent -> convertAndBuffer(batches, recordedEvent));

    toSummaryRegistry.all().forEach(s -> s.summarizeAndReset().forEach(batches::addMetric));

    logger.info("This conversion had " + eventCount.size() + " events");
    logger.debug("Detailed view of event counts:  " + eventCount.toString());
    eventCount.clear();

    return batches;
  }

  private void updateStatistics(RecordedEvent event) {
    var name = event.getEventType().getName();
    if (eventCount.get(name) == null) {
      eventCount.put(name, new AtomicInteger(0));
    }
    eventCount.get(name).incrementAndGet();
  }

  private void convertAndBuffer(BufferedTelemetry batches, RecordedEvent event) {
    updateStatistics(event);

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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Attributes commonAttributes;
    private ToMetricRegistry toMetricRegistry;
    private ToSummaryRegistry toSummaryRegistry;
    private ToEventRegistry toEventRegistry;

    public Builder commonAttributes(Attributes commonAttributes) {
      this.commonAttributes = commonAttributes;
      return this;
    }

    public Builder metricMappers(ToMetricRegistry registry) {
      this.toMetricRegistry = registry;
      return this;
    }

    public Builder summaryMappers(ToSummaryRegistry registry) {
      this.toSummaryRegistry = registry;
      return this;
    }

    public Builder eventMapper(ToEventRegistry registry) {
      this.toEventRegistry = registry;
      return this;
    }

    public EventConverter build() {
      if (commonAttributes == null) {
        throw new IllegalStateException("Common attributes are required");
      }
      return new EventConverter(this);
    }
  }
}
