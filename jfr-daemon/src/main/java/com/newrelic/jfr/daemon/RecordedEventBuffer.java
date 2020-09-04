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

  public void bufferEvents(Path dumpFile, RecordingFile file) throws IOException {
    ctx.resetForNewFile();
    logger.debug("Looking in " + dumpFile + " for events after: " + ctx.getLastSeen());
    while (file.hasMoreEvents()) {
      var event = file.readEvent();
      handleEvent(event);
    }
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

  private void handleEvent(RecordedEvent event) {
    ctx.update(event);
    if (event.getStartTime().isAfter(ctx.getLastSeen())) {
      enqueue(event);
    }
  }

  private void enqueue(RecordedEvent event) {
    try {
      queue.put(event);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Error adding raw event to queue", e);
    }
  }

  public Stream<RecordedEvent> drainToStream() {
    var list = new ArrayList<RecordedEvent>(queue.size());
    queue.drainTo(list);
    return list.stream();
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
