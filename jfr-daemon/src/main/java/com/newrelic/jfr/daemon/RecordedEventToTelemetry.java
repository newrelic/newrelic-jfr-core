package com.newrelic.jfr.daemon;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.daemon.buffer.BufferedTelemetry;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.jfr.consumer.RecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordedEventToTelemetry {

  private static final Logger logger =
      LoggerFactory.getLogger(RecordedEventToTelemetry.class.getName());

  private final ToMetricRegistry toMetricRegistry;
  private final ToEventRegistry toEventRegistry;
  private final ToSummaryRegistry toSummaryRegistry;

  private final Map<String, AtomicInteger> eventCount = new HashMap<>();

  public RecordedEventToTelemetry(Builder builder) {
    this.toMetricRegistry = builder.toMetricRegistry;
    this.toEventRegistry = builder.toEventRegistry;
    this.toSummaryRegistry = builder.toSummaryRegistry;
  }

  public void processEvent(RecordedEvent event, BufferedTelemetry bufferedTelemetry) {
    if (event == null) {
      return;
    }
    convertAndBufferSafely(event, bufferedTelemetry);
  }

  private void convertAndBufferSafely(RecordedEvent event, BufferedTelemetry batches) {
    try {
      convertAndBuffer(event, batches);
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

  private void convertAndBuffer(RecordedEvent event, BufferedTelemetry batches) {
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
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ToMetricRegistry toMetricRegistry;
    private ToEventRegistry toEventRegistry;
    private ToSummaryRegistry toSummaryRegistry;

    public Builder metricMappers(ToMetricRegistry registry) {
      this.toMetricRegistry = registry;
      return this;
    }

    public Builder summaryMappers(ToSummaryRegistry registry) {
      this.toSummaryRegistry = registry;
      return this;
    }

    public Builder eventMappers(ToEventRegistry registry) {
      this.toEventRegistry = registry;
      return this;
    }

    public RecordedEventToTelemetry build() {
      return new RecordedEventToTelemetry(this);
    }
  }
}
