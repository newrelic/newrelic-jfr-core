package com.newrelic.jfr.daemon;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.telemetry.Attributes;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Processes all events in a RecordingFile and outputs the group of buffered batches. */
public class FileToBufferedTelemetry {

  private static final Logger logger =
      LoggerFactory.getLogger(FileToBufferedTelemetry.class.getName());

  private final Attributes commonAttributes;
  private final ToMetricRegistry toMetricRegistry;
  private final ToSummaryRegistry toSummaryRegistry;
  private final ToEventRegistry toEventRegistry;

  private final Map<String, AtomicInteger> eventCount = new HashMap<>();

  public FileToBufferedTelemetry(Builder builder) {
    this.commonAttributes = builder.commonAttributes;
    this.toMetricRegistry = builder.toMetricRegistry;
    this.toSummaryRegistry = builder.toSummaryRegistry;
    this.toEventRegistry = builder.toEventRegistry;
  }

  /**
   * Converts the given recording file int a result that contains the last seen time in the file and
   * batched metric data.
   *
   * @param file The input RecordingFile to convert
   * @param onlyAfter Only consider events whose time is after this
   * @param dumpFilename Write contents into a file at this filename
   * @throws IOException if reading/writing encounters a problem
   * @return a Result containing BufferedTelemetry and the Instant of the last seen event time
   */
  public Result convert(RecordingFile file, Instant onlyAfter, String dumpFilename)
      throws IOException {
    var batches = BufferedTelemetry.create(commonAttributes);
    ProcessingContext ctx = new ProcessingContext(onlyAfter);

    while (file.hasMoreEvents()) {
      var event = file.readEvent();
      processEvent(batches, ctx, event);
    }
    // Loop over the summarizers and add the summaries to the buffer
    toSummaryRegistry.all().forEach(s -> s.summarizeAndReset().forEach(batches::addMetric));

    logger.debug(
        "File contains events from: "
            + ctx.getFirstEventTime()
            + " to "
            + ctx.getLastEventTime()
            + " ["
            + ctx.getLastSeen()
            + "]"
            + " in "
            + dumpFilename);

    logger.debug(eventCount.toString());
    eventCount.clear();

    return new Result(ctx.getLastEventTime(), batches);
  }

  private void processEvent(BufferedTelemetry batches, ProcessingContext ctx, RecordedEvent event) {
    if (event == null) {
      return;
    }
    ctx.updateFirstEventTime(event);
    ctx.updateLastEventTime(event);

    if (event.getStartTime().isAfter(ctx.getLastSeen())) {
      convertAndBuffer(batches, event);
    }
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

  public static class Result {
    private final Instant lastSeen;
    private final BufferedTelemetry bufferedTelemetry;

    public Result(Instant lastSeen, BufferedTelemetry bufferedTelemetry) {
      this.lastSeen = lastSeen;
      this.bufferedTelemetry = bufferedTelemetry;
    }

    public Instant getLastSeen() {
      return lastSeen;
    }

    public BufferedTelemetry getBufferedTelemetry() {
      return bufferedTelemetry;
    }
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

    public FileToBufferedTelemetry build() {
      if (commonAttributes == null) {
        logger.warn("Warning: defaulting common attributes.");
        commonAttributes = new Attributes();
      }
      return new FileToBufferedTelemetry(this);
    }
  }

  private static class ProcessingContext {
    private Instant firstEventTime = null;
    private Instant lastEventTime = null;
    private final Instant lastSeen;

    public ProcessingContext(Instant lastSeen) {
      this.lastSeen = lastSeen;
    }

    public void updateFirstEventTime(RecordedEvent event) {
      if (firstEventTime == null) {
        firstEventTime = event.getStartTime();
      }
    }

    public void updateLastEventTime(RecordedEvent event) {
      lastEventTime = event.getStartTime();
    }

    public Instant getFirstEventTime() {
      return firstEventTime;
    }

    public Instant getLastEventTime() {
      return lastEventTime;
    }

    public Instant getLastSeen() {
      return lastSeen;
    }
  }
}
