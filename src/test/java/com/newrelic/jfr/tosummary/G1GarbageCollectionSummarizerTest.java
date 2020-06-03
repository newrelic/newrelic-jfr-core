package com.newrelic.jfr.tosummary;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class G1GarbageCollectionSummarizerTest {

  private static Summary defaultSummary;

  @BeforeAll
  static void init() {
    defaultSummary =
        new Summary(
            "jfr:ObjectAllocationInNewTLAB.allocation",
            0,
            Duration.ofNanos(0L).toMillis(),
            Duration.ofNanos(Long.MAX_VALUE).toMillis(),
            Duration.ofNanos(0L).toMillis(),
            Instant.now().toEpochMilli(),
            0L,
            new Attributes());
  }

  @Test
  void testSingleEventSummaryAndReset() {
    var summaryStartTime = Instant.now().toEpochMilli();

    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = summaryStartTime + 1;
    var eventDurationNanos = 13700000;
    var eventDurationMillis = Duration.ofNanos(eventDurationNanos).toMillis();

    var expectedSummaryMetric =
        new Summary(
            "jfr:G1GarbageCollection.duration",
            numOfEvents, // count
            eventDurationMillis, // sum
            eventDurationMillis, // min
            eventDurationMillis, // max
            summaryStartTime, // startTimeMs
            eventStartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of each
                            // RecordedEvent
            new Attributes());

    List<Metric> expected = List.of(expectedSummaryMetric);
    var testClass = new G1GarbageCollectionSummarizer(summaryStartTime);

    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    testClass.accept(event);
    final List<Summary> result = testClass.summarizeAndReset().collect(toList());
    final Summary resetResultSummary = testClass.summarizeAndReset().collect(toList()).get(0);

    assertEquals(expected, result);

    // Summary should be reset to default values
    assertEquals(defaultSummary.getCount(), resetResultSummary.getCount());
    assertEquals(defaultSummary.getSum(), resetResultSummary.getSum());
    assertEquals(defaultSummary.getMin(), resetResultSummary.getMin());
    assertEquals(defaultSummary.getMax(), resetResultSummary.getMax());
  }

  @Test
  void testMultipleEventSummaryAndReset() {
    var summaryStartTime = Instant.now().toEpochMilli();

    var event1 = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var event1StartTime = summaryStartTime + 1;
    var event1DurationNanos = 13700000;

    var event2 = mock(RecordedEvent.class);
    numOfEvents = ++numOfEvents;
    var event2StartTime = summaryStartTime + 2;
    var event2DurationNanos = 24800000; // max duration of final summary
    var event2DurationMillis = Duration.ofNanos(event2DurationNanos).toMillis();

    var event3 = mock(RecordedEvent.class);
    numOfEvents = ++numOfEvents;
    var event3StartTime = summaryStartTime + 3;
    var event3DurationNanos = 1000000; // min duration of final summary
    var event3DurationMillis = Duration.ofNanos(event3DurationNanos).toMillis();

    var summedDurationNanos = event1DurationNanos + event2DurationNanos + event3DurationNanos;
    var summedDurationMillis = Duration.ofNanos(summedDurationNanos).toMillis();

    var expectedSummaryMetric =
        new Summary(
            "jfr:G1GarbageCollection.duration",
            numOfEvents, // count
            summedDurationMillis, // sum
            event3DurationMillis, // min
            event2DurationMillis, // max
            summaryStartTime, // startTimeMs
            event3StartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of
            // each RecordedEvent
            new Attributes());

    var expected = List.of(expectedSummaryMetric);

    var testClass = new G1GarbageCollectionSummarizer(summaryStartTime);

    when(event1.getStartTime()).thenReturn(Instant.ofEpochMilli(event1StartTime));
    when(event1.getDuration("duration")).thenReturn(Duration.ofNanos(event1DurationNanos));

    when(event2.getStartTime()).thenReturn(Instant.ofEpochMilli(event2StartTime));
    when(event2.getDuration("duration")).thenReturn(Duration.ofNanos(event2DurationNanos));

    when(event3.getStartTime()).thenReturn(Instant.ofEpochMilli(event3StartTime));
    when(event3.getDuration("duration")).thenReturn(Duration.ofNanos(event3DurationNanos));

    // Summarize all events
    testClass.accept(event1);
    testClass.accept(event2);
    testClass.accept(event3);

    final List<Summary> result = testClass.summarizeAndReset().collect(toList());
    assertEquals(expected, result);

    final Summary resetResultSummary = testClass.summarizeAndReset().collect(toList()).get(0);

    // Summary should be reset to default values
    assertEquals(defaultSummary.getCount(), resetResultSummary.getCount());
    assertEquals(defaultSummary.getSum(), resetResultSummary.getSum());
    assertEquals(defaultSummary.getMin(), resetResultSummary.getMin());
    assertEquals(defaultSummary.getMax(), resetResultSummary.getMax());
  }
}
