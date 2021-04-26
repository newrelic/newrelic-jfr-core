package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.tosummary.G1GarbageCollectionSummarizer.JFR_G1_GARBAGE_COLLECTION_DURATION;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class G1GarbageCollectionSummarizerTest {

  private static Summary defaultSummary;
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;
  private static final String DURATION = "duration";
  private static final String JFR_OBJECT_ALLOCATION_IN_NEW_TLAB_ALLOCATION =
      "jfr.ObjectAllocationInNewTLAB.allocation";

  @BeforeAll
  static void init() {
    defaultSummary =
        new Summary(
            JFR_OBJECT_ALLOCATION_IN_NEW_TLAB_ALLOCATION,
            0,
            Duration.ofNanos(0L).toMillis(),
            Duration.ofNanos(Long.MAX_VALUE).toMillis(),
            Duration.ofNanos(Long.MIN_VALUE).toMillis(),
            Instant.now().toEpochMilli(),
            0L,
            new Attributes());

    recordedObjectValidatorsMockedStatic = Mockito.mockStatic(RecordedObjectValidators.class);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.hasField(
                    any(RecordedObject.class), anyString(), anyString()))
        .thenReturn(true);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.isRecordedObjectNull(
                    any(RecordedObject.class), anyString()))
        .thenReturn(false);
  }

  @AfterAll
  static void teardown() {
    recordedObjectValidatorsMockedStatic.close();
  }

  @Test
  void testSingleEventSummary() {
    var summaryStartTime = Instant.now().toEpochMilli();

    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = summaryStartTime + 1;
    var eventDurationNanos = 13700000;
    var eventDurationMillis = Duration.ofNanos(eventDurationNanos).toMillis();

    var expectedSummaryMetric =
        new Summary(
            JFR_G1_GARBAGE_COLLECTION_DURATION,
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
    when(event.getDuration(DURATION)).thenReturn(Duration.ofNanos(eventDurationNanos));

    testClass.accept(event);
    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(expected, result);
  }

  @Test
  void testMultipleEventSummary() {
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
            JFR_G1_GARBAGE_COLLECTION_DURATION,
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
    when(event1.getDuration(DURATION)).thenReturn(Duration.ofNanos(event1DurationNanos));

    when(event2.getStartTime()).thenReturn(Instant.ofEpochMilli(event2StartTime));
    when(event2.getDuration(DURATION)).thenReturn(Duration.ofNanos(event2DurationNanos));

    when(event3.getStartTime()).thenReturn(Instant.ofEpochMilli(event3StartTime));
    when(event3.getDuration(DURATION)).thenReturn(Duration.ofNanos(event3DurationNanos));

    // Summarize all events
    testClass.accept(event1);
    testClass.accept(event2);
    testClass.accept(event3);

    var result = testClass.summarize().collect(toList());
    assertEquals(expected, result);
  }

  @Test
  void testReset() {
    var summaryStartTime = Instant.now().toEpochMilli();

    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = summaryStartTime + 1;
    var eventDurationNanos = 13700000;
    var eventDurationMillis = Duration.ofNanos(eventDurationNanos).toMillis();

    var expectedSummaryMetric =
        new Summary(
            JFR_G1_GARBAGE_COLLECTION_DURATION,
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
    when(event.getDuration(DURATION)).thenReturn(Duration.ofNanos(eventDurationNanos));

    testClass.accept(event);
    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(expected, result);

    testClass.reset();
    final Summary resetResultSummary = testClass.summarize().collect(toList()).get(0);

    // Summary should be reset to default values
    assertEquals(defaultSummary.getCount(), resetResultSummary.getCount());
    assertEquals(defaultSummary.getSum(), resetResultSummary.getSum());
    assertEquals(defaultSummary.getMin(), resetResultSummary.getMin());
    assertEquals(defaultSummary.getMax(), resetResultSummary.getMax());
  }
}
