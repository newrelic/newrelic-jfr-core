/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class adds RecordedEvents from a RecordingFile to a queue. It statefully keeps track of the
 * time of the event seen from the previous file and uses this to prevent queueing duplicates.
 */
public class RecordedEventBuffer {

  private static final Logger logger = LoggerFactory.getLogger(RecordedEventBuffer.class);

  private final BlockingQueue<RecordedEvent> queue;
  private final RawProcessingContext ctx = new RawProcessingContext();

  public RecordedEventBuffer(BlockingQueue<RecordedEvent> queue) {
    this.queue = queue;
  }

  /**
   * Buffer the events of the {@code file} to the {@link #queue}. Iterate through the events of the
   * JFR {@code file}, filtering events older than the last seen watermark, and add to the queue
   * until there are no more events or the queue is full.
   *
   * @param dumpFile the path of the {@code file}
   * @param file the JFR file
   * @throws IOException if an error occurs reading the file
   */
  public void bufferEvents(Path dumpFile, RecordingFile file) throws IOException {
    ctx.resetForNewFile();
    if (logger.isDebugEnabled()) {
      logger.debug("Looking in " + dumpFile + " for events after: " + ctx.getLastSeen());
    }
    while (file.hasMoreEvents()) {
      RecordedEvent event = file.readEvent();
      if (!handleEvent(event)) {
        logger.warn("Ignoring remaining events in this file due to full queue!");
        break;
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Queued events from: "
              + ctx.getFirstEventTime()
              + " to "
              + ctx.getLastEventTime()
              + " ["
              + ctx.getLastSeen()
              + "]"
              + " in "
              + dumpFile);
    }
  }

  private boolean handleEvent(RecordedEvent event) {
    ctx.update(event);
    if (event.getStartTime().isAfter(ctx.getLastSeen())) {
      return enqueue(event);
    }
    return true;
  }

  private boolean enqueue(RecordedEvent event) {
    boolean success = queue.offer(event);
    if (!success) {
      logger.error("Rejecting RecordedEvent -- queue is full!!!");
    }
    return success;
  }

  public Stream<RecordedEvent> drainToStream() {
    List<RecordedEvent> list = new ArrayList<>(queue.size());
    queue.drainTo(list);
    return list.stream();
  }

  public Instant start() {
    return ctx.getFirstEventTime();
  }

  public Instant end() {
    return ctx.getLastEventTime();
  }

  static class RawProcessingContext {
    private Instant firstEventTime = null;
    private Instant lastEventTime = null;
    private Instant lastSeen;

    public RawProcessingContext() {
      this(Instant.EPOCH);
    }

    public RawProcessingContext(Instant lastSeen) {
      this.lastSeen = lastSeen;
    }

    public void update(RecordedEvent event) {
      updateFirstEventTime(event);
      updateLastEventTime(event);
    }

    private void updateFirstEventTime(RecordedEvent event) {
      if (firstEventTime == null) {
        firstEventTime = event.getStartTime();
      }
    }

    private void updateLastEventTime(RecordedEvent event) {
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

    public void resetForNewFile() {
      lastSeen = lastEventTime == null ? Instant.EPOCH : lastEventTime;
    }
  }
}
