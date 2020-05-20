package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

public class PerThreadNetworkWriteSummarizer implements EventToSummary {
  private final String threadName;
  private long startTimeMs;
  private long endTimeMs = 0L;
  private int count = 0;
  private long bytes = 0L;
  private long minBytes = Long.MAX_VALUE;
  private long maxBytes = 0L;
  private Duration duration = Duration.ofNanos(0L);
  private Duration minDuration = Duration.ofNanos(Long.MAX_VALUE);
  private Duration maxDuration = Duration.ofNanos(0L);

  public PerThreadNetworkWriteSummarizer(String threadName, long startTimeMs) {
    this.threadName = threadName;
    this.startTimeMs = startTimeMs;
  }

  @Override
  public String getEventName() {
    return NetworkWriteSummarizer.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    var duration = ev.getDuration();
    endTimeMs = ev.getStartTime().plus(duration).toEpochMilli();
    count++;
    var bytesWritten = ev.getLong("bytesWritten");
    bytes = bytes + bytesWritten;

    if (bytesWritten > maxBytes) {
      maxBytes = bytesWritten;
    }
    if (bytesWritten < minBytes) {
      minBytes = bytesWritten;
    }

    this.duration = this.duration.plus(duration);

    if (duration.compareTo(maxDuration) > 0) {
      maxDuration = duration;
    }
    if (duration.compareTo(minDuration) < 0) {
      minDuration = duration;
    }
  }

  @Override
  public Stream<Summary> summarizeAndReset() {
    var attr = new Attributes().put("thread.name", threadName);
    var outWritten =
        new Summary(
            "jfr:SocketWrite.bytesWritten",
            count,
            bytes,
            minBytes,
            maxBytes,
            startTimeMs,
            endTimeMs,
            attr);
    var outDuration =
        new Summary(
            "jfr:SocketWrite.duration",
            count,
            duration.toMillis(),
            minDuration.toMillis(),
            maxDuration.toMillis(),
            startTimeMs,
            endTimeMs,
            attr);

    reset();
    return Stream.of(outWritten, outDuration);
  }

  public void reset() {
    startTimeMs = Instant.now().toEpochMilli();
    endTimeMs = 0L;
    count = 0;
    bytes = 0L;
    minBytes = Long.MAX_VALUE;
    maxBytes = 0L;
    duration = Duration.ofNanos(0L);
    minDuration = Duration.ofNanos(Long.MAX_VALUE);
    maxDuration = Duration.ofNanos(0L);
  }
}
