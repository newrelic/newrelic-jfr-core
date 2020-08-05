package com.newrelic.jfr.daemon;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the source of all RecordedEvents. It processes instances of RecordingFile and sends ones
 * later than the previously seen on to the consumer.
 */
public class FileToRawEventSource {

  private static final Logger logger =
      LoggerFactory.getLogger(FileToRawEventSource.class.getName());

  private final Consumer<RecordedEvent> eventConsumer;
  private final Map<String, AtomicInteger> eventCount = new HashMap<>();

  public FileToRawEventSource(Consumer<RecordedEvent> eventConsumer) {
    this.eventConsumer = eventConsumer;
  }

  /**
   * Pulls all raw RecordedEvents out of a RecordingFile and a result that contains the last seen
   * time in the file and batched metric data.
   *
   * @param file The input RecordingFile to convert
   * @param onlyAfter Only consider events whose time is after this
   * @param dumpFilename Write contents into a file at this filename
   * @throws IOException if reading/writing encounters a problem
   * @return a Result containing BufferedTelemetry and the Instant of the last seen event time
   */
  public Instant queueRawEvents(RecordingFile file, Instant onlyAfter, String dumpFilename)
      throws IOException {
    ProcessingContext ctx = new ProcessingContext(onlyAfter);

    while (file.hasMoreEvents()) {
      var event = file.readEvent();
      processEvent(ctx, event);
    }

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

    logger.info(eventCount.toString());
    eventCount.clear();

    return ctx.getLastEventTime();
  }

  private void processEvent(ProcessingContext ctx, RecordedEvent event) {
    if (event == null) {
      return;
    }
    ctx.updateFirstEventTime(event);
    ctx.updateLastEventTime(event);

    if (event.getStartTime().isAfter(ctx.getLastSeen())) {
      eventConsumer.accept(event);
      updateStatistics(event);
    }
  }

  private void updateStatistics(RecordedEvent event) {
    var name = event.getEventType().getName();
    if (eventCount.get(name) == null) {
      eventCount.put(name, new AtomicInteger(0));
    }
    eventCount.get(name).incrementAndGet();
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
