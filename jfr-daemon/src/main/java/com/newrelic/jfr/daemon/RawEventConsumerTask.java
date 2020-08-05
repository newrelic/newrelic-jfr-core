package com.newrelic.jfr.daemon;

import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.daemon.buffer.BufferedTelemetry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import jdk.jfr.consumer.RecordedEvent;

public class RawEventConsumerTask implements Runnable {
  private final BlockingQueue<RecordedEvent> rawEventQueue;
  private final BufferedTelemetry bufferedTelemetry;
  private final RecordedEventToTelemetry recordedEventToTelemetry;
  private final LastSendStateTracker lastSendStateTracker;
  private final ToSummaryRegistry toSummaryRegistry;
  private final JFRUploader uploader;
  private volatile boolean shutdown = false;

  private RawEventConsumerTask(Builder builder) {
    this.rawEventQueue = builder.rawEventQueue;
    this.bufferedTelemetry = builder.bufferedTelemetry;
    this.recordedEventToTelemetry = builder.recordedEventToTelemetry;
    this.lastSendStateTracker = builder.lastSendStateTracker;
    this.toSummaryRegistry = builder.toSummaryRegistry;
    this.uploader = builder.uploader;
  }

  @Override
  public void run() {
    while (!shutdown) {
      var event = pollSafely();
      if (event != null) {
        recordedEventToTelemetry.processEvent(event, bufferedTelemetry);
      }
      if (shouldSendNow()) {
        finishSummarizers();
        uploader.send(bufferedTelemetry);
      }
    }
  }

  public boolean isShutdown() {
    return shutdown;
  }

  private void finishSummarizers() {
    toSummaryRegistry
        .all()
        .forEach(s -> s.summarizeAndReset().forEach(bufferedTelemetry::addMetric));
  }

  private boolean shouldSendNow() {
    return lastSendStateTracker.updateIfReady(bufferedTelemetry.getTotalSize());
  }

  private RecordedEvent pollSafely() {
    try {
      return rawEventQueue.poll(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      shutdown();
      Thread.currentThread().interrupt();
      return null;
    }
  }

  public void shutdown() {
    shutdown = true;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private BlockingQueue<RecordedEvent> rawEventQueue;
    private BufferedTelemetry bufferedTelemetry;
    private RecordedEventToTelemetry recordedEventToTelemetry;
    private LastSendStateTracker lastSendStateTracker;
    private ToSummaryRegistry toSummaryRegistry;
    private JFRUploader uploader;

    public Builder rawEventQueue(BlockingQueue<RecordedEvent> rawEventQueue) {
      this.rawEventQueue = rawEventQueue;
      return this;
    }

    public Builder bufferedTelemetry(BufferedTelemetry bufferedTelemetry) {
      this.bufferedTelemetry = bufferedTelemetry;
      return this;
    }

    public Builder recordedEventToTelemetry(RecordedEventToTelemetry recordedEventToTelemetry) {
      this.recordedEventToTelemetry = recordedEventToTelemetry;
      return this;
    }

    public Builder lastSendStateTracker(LastSendStateTracker lastSendStateTracker) {
      this.lastSendStateTracker = lastSendStateTracker;
      return this;
    }

    public Builder toSummaryRegistry(ToSummaryRegistry toSummaryRegistry) {
      this.toSummaryRegistry = toSummaryRegistry;
      return this;
    }

    public Builder uploader(JFRUploader uploader) {
      this.uploader = uploader;
      return this;
    }

    public RawEventConsumerTask build() {
      return new RawEventConsumerTask(this);
    }
  }
}
