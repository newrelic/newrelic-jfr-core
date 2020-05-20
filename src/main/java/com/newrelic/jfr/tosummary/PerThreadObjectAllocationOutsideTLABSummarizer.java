package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Instant;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates all TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationOutsideTLABSummarizer implements EventToSummary {

  private final String threadName;
  private int count = 0;
  private long sum = 0L;
  private long min = Long.MAX_VALUE;
  private long max = 0L;
  private long startTimeMs;
  private long endTimeMs = 0L;

  public PerThreadObjectAllocationOutsideTLABSummarizer(String threadName, long startTimeMs) {
    this.threadName = threadName;
    this.startTimeMs = startTimeMs;
  }

  @Override
  public String getEventName() {
    return ObjectAllocationOutsideTLABSummarizer.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    endTimeMs = ev.getStartTime().toEpochMilli();
    count++;
    var alloc = ev.getLong("allocationSize");
    sum = sum + alloc;

    if (alloc > max) {
      max = alloc;
    }
    if (alloc < min) {
      min = alloc;
    }
    // Probably too high a cardinality
    // ev.getClass("objectClass").getName();
  }

  @Override
  public Stream<Summary> summarizeAndReset() {
    var attr = new Attributes().put("thread.name", threadName);
    var out =
        new Summary(
            "jfr:ObjectAllocationOutsideTLAB.allocation",
            count,
            sum,
            min,
            max,
            startTimeMs,
            endTimeMs,
            attr);
    reset();
    return Stream.of(out);
  }

  public void reset() {
    startTimeMs = Instant.now().toEpochMilli();
    endTimeMs = 0L;
    count = 0;
    sum = 0L;
    min = Long.MAX_VALUE;
    max = 0L;
  }
}
