package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GCHeapSummarySummarizerTest {

  private static Summary defaultSummary;

  @BeforeAll
  static void init() {
    defaultSummary =
        new Summary(
            "jfr:ObjectAllocationInNewTLAB.allocation",
            0,
            Duration.ofNanos(0L).toMillis(),
            Duration.ofNanos(Long.MAX_VALUE).toMillis(),
            Duration.ofNanos(Long.MIN_VALUE).toMillis(),
            Instant.now().toEpochMilli(),
            0L,
            new Attributes());
  }

  @Test
  void pairEventSummaryAndReset() {
    var summaryStartTime = Instant.now().toEpochMilli();

    var before = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var beforeStartTime = summaryStartTime + 1;
    var beforeDurationNanos = 13700000;
    var beforeDurationMillis = Duration.ofNanos(beforeDurationNanos).toMillis();

    when(before.getString("when")).thenReturn("Before GC");
    when(before.getStartTime()).thenReturn(Instant.ofEpochMilli(beforeStartTime));
    when(before.getDuration("duration")).thenReturn(Duration.ofNanos(beforeDurationNanos));


    var after = mock(RecordedEvent.class);
    numOfEvents = numOfEvents + 1;
    var afterStartTime = summaryStartTime + 2;
    var afterDurationNanos = 24800000; // max duration of final summary
    var afterDurationMillis = Duration.ofNanos(afterDurationNanos).toMillis();

    when(after.getString("when")).thenReturn("After GC");
    when(after.getStartTime()).thenReturn(Instant.ofEpochMilli(afterStartTime));
    when(after.getDuration("duration")).thenReturn(Duration.ofNanos(afterDurationNanos));


    var summedDurationNanos = beforeDurationNanos + afterDurationNanos;
    var summedDurationMillis = Duration.ofNanos(summedDurationNanos).toMillis();

    var expectedSummaryMetric =
            new Summary(
                    "jfr:G1GarbageCollection.duration",
                    numOfEvents, // count
                    summedDurationMillis, // sum
                    beforeDurationMillis, // min
                    afterDurationMillis, // max
                    summaryStartTime, // startTimeMs
                    afterStartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of
                    // each RecordedEvent
                    new Attributes());

    var expected = List.of(expectedSummaryMetric);

    var testClass = new GCHeapSummarySummarizer(summaryStartTime);

    // Summarize all events
    testClass.accept(before);
    testClass.accept(after);

    var result = testClass.summarizeAndReset().collect(toList());
    assertEquals(expected, result);

    var resetResultSummary = testClass.summarizeAndReset().collect(toList()).get(0);

    // Summary should be reset to default values
    assertEquals(defaultSummary.getCount(), resetResultSummary.getCount());
    assertEquals(defaultSummary.getSum(), resetResultSummary.getSum());
    assertEquals(defaultSummary.getMin(), resetResultSummary.getMin());
    assertEquals(defaultSummary.getMax(), resetResultSummary.getMax());

  }

}
