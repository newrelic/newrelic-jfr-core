package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Instant;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

/** This class aggregates all TLAB allocation JFR events for a single thread */
public final class PerThreadObjectAllocationOutsideTLABSummarizer implements EventToSummary {

  private final String threadName;
  private final LongSummarizer summarizer;
  private long startTimeMs;
  private long endTimeMs = 0L;

  public PerThreadObjectAllocationOutsideTLABSummarizer(String threadName, long startTimeMs) {
    this(threadName, startTimeMs, new LongSummarizer("allocationSize"));
  }

  public PerThreadObjectAllocationOutsideTLABSummarizer(
      String threadName, long startTimeMs, LongSummarizer summarizer) {
    this.threadName = threadName;
    this.startTimeMs = startTimeMs;
    this.summarizer = summarizer;
  }

  @Override
  public String getEventName() {
    return ObjectAllocationOutsideTLABSummarizer.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    endTimeMs = ev.getStartTime().toEpochMilli();
    summarizer.accept(ev);
    // Probably too high a cardinality
    // ev.getClass("objectClass").getName();
  }

  @Override
  public Stream<Summary> summarizeAndReset() {
    var attr = new Attributes().put("thread.name", threadName);
    var out =
        new Summary(
            "jfr:ObjectAllocationOutsideTLAB.allocation",
            summarizer.getCount(),
            summarizer.getSum(),
            summarizer.getMin(),
            summarizer.getMax(),
            startTimeMs,
            endTimeMs,
            attr);
    reset();
    return Stream.of(out);
  }

  public void reset() {
    startTimeMs = Instant.now().toEpochMilli();
    endTimeMs = 0L;
    summarizer.reset();
  }
}
