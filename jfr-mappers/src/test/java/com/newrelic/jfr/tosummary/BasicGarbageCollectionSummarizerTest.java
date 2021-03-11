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
import java.util.concurrent.atomic.AtomicLong;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BasicGarbageCollectionSummarizerTest {
  private static Summary defaultMinorGcSummary;
  private static Summary defaultMajorGcSummary;
  private static final long DEFAULT_SUM = Duration.ofNanos(0L).toMillis();
  private static final long DEFAULT_MIN = Duration.ofNanos(Long.MAX_VALUE).toMillis();
  private static final long DEFAULT_MAX = Duration.ofNanos(Long.MIN_VALUE).toMillis();
  private static final int DEFAULT_COUNT = 0;
  private static final long DEFAULT_START_TIME_MS = Instant.now().toEpochMilli();
  private static final long DEFAULT_END_TIME_MS = 0L;
  private static final String MINOR_GC_DURATION_METRIC_NAME = "jfr.GarbageCollection.minorDuration";
  private static final String MAJOR_GC_DURATION_METRIC_NAME = "jfr.GarbageCollection.majorDuration";

  @BeforeAll
  static void init() {
    defaultMinorGcSummary =
        new Summary(
            MINOR_GC_DURATION_METRIC_NAME,
            DEFAULT_COUNT,
            DEFAULT_SUM,
            DEFAULT_MIN,
            DEFAULT_MAX,
            DEFAULT_START_TIME_MS,
            DEFAULT_END_TIME_MS,
            new Attributes());

    defaultMajorGcSummary =
        new Summary(
            MAJOR_GC_DURATION_METRIC_NAME,
            DEFAULT_COUNT,
            DEFAULT_SUM,
            DEFAULT_MIN,
            DEFAULT_MAX,
            DEFAULT_START_TIME_MS,
            DEFAULT_END_TIME_MS,
            new Attributes());
  }

  @Test
  void testSingleMinorGcEventSummary() {
    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = DEFAULT_START_TIME_MS + 1;
    var eventDurationNanos = 13_700_000;
    var eventDurationMillis = Duration.ofNanos(eventDurationNanos).toMillis();

    var expectedMinorGcSummaryMetric =
        new Summary(
            MINOR_GC_DURATION_METRIC_NAME,
            numOfEvents, // count
            eventDurationMillis, // sum
            eventDurationMillis, // min
            eventDurationMillis, // max
            DEFAULT_START_TIME_MS, // startTimeMs
            eventStartTime, // endTimeMs
            new Attributes());

    List<Metric> expected = List.of(expectedMinorGcSummaryMetric, defaultMajorGcSummary);
    var testClass = new BasicGarbageCollectionSummarizer(DEFAULT_START_TIME_MS);

    when(event.getValue("name")).thenReturn("G1New");
    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    testClass.accept(event);
    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(expected, result);

    for (Summary summary : result) {
      var summaryName = summary.getName();
      if (summaryName.equals(MINOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(numOfEvents, summary.getCount());
      } else if (summaryName.equals(MAJOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(DEFAULT_COUNT, summary.getCount());
      }
    }
  }

  @Test
  void testSingleMajorGcEventSummary() {
    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = DEFAULT_START_TIME_MS + 1;
    var eventDurationNanos = 13_700_000;
    var eventDurationMillis = Duration.ofNanos(eventDurationNanos).toMillis();

    var expectedMajorGcSummaryMetric =
        new Summary(
            MAJOR_GC_DURATION_METRIC_NAME,
            numOfEvents, // count
            eventDurationMillis, // sum
            eventDurationMillis, // min
            eventDurationMillis, // max
            DEFAULT_START_TIME_MS, // startTimeMs
            eventStartTime, // endTimeMs
            new Attributes());

    List<Metric> expected = List.of(defaultMinorGcSummary, expectedMajorGcSummaryMetric);
    var testClass = new BasicGarbageCollectionSummarizer(DEFAULT_START_TIME_MS);

    when(event.getValue("name")).thenReturn("G1Old");
    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    testClass.accept(event);
    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(expected, result);

    for (Summary summary : result) {
      var summaryName = summary.getName();
      if (summaryName.equals(MAJOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(numOfEvents, summary.getCount());
      } else if (summaryName.equals(MINOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(DEFAULT_COUNT, summary.getCount());
      }
    }
  }

  @Test
  void testMultipleGcEventSummariesOfBothTypesAndReset() {
    var numOfMinorGcEvents = 4;
    var minorGcEvent1 = mock(RecordedEvent.class);
    var minorGcEvent2 = mock(RecordedEvent.class);
    var minorGcEvent3 = mock(RecordedEvent.class);
    var minorGcEvent4 = mock(RecordedEvent.class);

    var numOfMajorGcEvents = 4;
    var majorGcEvent5 = mock(RecordedEvent.class);
    var majorGcEvent6 = mock(RecordedEvent.class);
    var majorGcEvent7 = mock(RecordedEvent.class);
    var majorGcEvent8 = mock(RecordedEvent.class);

    var startTimeEvent1 = new AtomicLong(DEFAULT_START_TIME_MS + 1);
    var startTimeEvent2 = new AtomicLong(startTimeEvent1.incrementAndGet());
    var startTimeEvent3 = new AtomicLong(startTimeEvent2.incrementAndGet());
    var startTimeEvent4 = new AtomicLong(startTimeEvent3.incrementAndGet());
    var startTimeEvent5 = new AtomicLong(startTimeEvent4.incrementAndGet());
    var startTimeEvent6 = new AtomicLong(startTimeEvent5.incrementAndGet());
    var startTimeEvent7 = new AtomicLong(startTimeEvent6.incrementAndGet());
    var startTimeEvent8 = new AtomicLong(startTimeEvent7.incrementAndGet());

    var eventDurationNanos = 13_700_000;
    var eventDurationMillis = Duration.ofNanos(eventDurationNanos).toMillis();
    var minorGcDurationSum = Duration.ofNanos(eventDurationNanos * numOfMinorGcEvents).toMillis();
    var majorGcDurationSum = Duration.ofNanos(eventDurationNanos * numOfMajorGcEvents).toMillis();

    var expectedMinorGcSummaryMetric =
        new Summary(
            MINOR_GC_DURATION_METRIC_NAME,
            numOfMinorGcEvents, // count
            minorGcDurationSum, // sum
            eventDurationMillis, // min
            eventDurationMillis, // max
            DEFAULT_START_TIME_MS, // startTimeMs
            startTimeEvent4.get(), // endTimeMs
            new Attributes());

    var expectedMajorGcSummaryMetric =
        new Summary(
            MAJOR_GC_DURATION_METRIC_NAME,
            numOfMajorGcEvents, // count
            majorGcDurationSum, // sum
            eventDurationMillis, // min
            eventDurationMillis, // max
            DEFAULT_START_TIME_MS, // startTimeMs
            startTimeEvent8.get(), // endTimeMs
            new Attributes());

    List<Metric> expected = List.of(expectedMinorGcSummaryMetric, expectedMajorGcSummaryMetric);
    var testClass = new BasicGarbageCollectionSummarizer(DEFAULT_START_TIME_MS);

    // minor GC events
    when(minorGcEvent1.getValue("name")).thenReturn("ParNew");
    when(minorGcEvent1.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent1.get()));
    when(minorGcEvent1.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    when(minorGcEvent2.getValue("name")).thenReturn("DefNew");
    when(minorGcEvent2.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent2.get()));
    when(minorGcEvent2.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    when(minorGcEvent3.getValue("name")).thenReturn("PSMarkSweep");
    when(minorGcEvent3.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent3.get()));
    when(minorGcEvent3.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    when(minorGcEvent4.getValue("name")).thenReturn("ParallelScavenge");
    when(minorGcEvent4.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent4.get()));
    when(minorGcEvent4.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    // major GC events
    when(majorGcEvent5.getValue("name")).thenReturn("ParallelOld");
    when(majorGcEvent5.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent5.get()));
    when(majorGcEvent5.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    when(majorGcEvent6.getValue("name")).thenReturn("SerialOld");
    when(majorGcEvent6.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent6.get()));
    when(majorGcEvent6.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    when(majorGcEvent7.getValue("name")).thenReturn("ConcurrentMarkSweep");
    when(majorGcEvent7.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent7.get()));
    when(majorGcEvent7.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    when(majorGcEvent8.getValue("name")).thenReturn("G1Full");
    when(majorGcEvent8.getStartTime()).thenReturn(Instant.ofEpochMilli(startTimeEvent8.get()));
    when(majorGcEvent8.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    testClass.accept(minorGcEvent1);
    testClass.accept(minorGcEvent2);
    testClass.accept(minorGcEvent3);
    testClass.accept(minorGcEvent4);

    testClass.accept(majorGcEvent5);
    testClass.accept(majorGcEvent6);
    testClass.accept(majorGcEvent7);
    testClass.accept(majorGcEvent8);

    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(expected, result);

    for (Summary summary : result) {
      var summaryName = summary.getName();
      if (summaryName.equals(MINOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(numOfMinorGcEvents, summary.getCount());
      } else if (summaryName.equals(MAJOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(numOfMajorGcEvents, summary.getCount());
      }
    }

    testClass.reset();
    final Summary resetResultSummary = testClass.summarize().collect(toList()).get(0);

    // Minor and Major GC summaries should be reset to default values
    assertEquals(defaultMinorGcSummary.getCount(), resetResultSummary.getCount());
    assertEquals(defaultMinorGcSummary.getSum(), resetResultSummary.getSum());
    assertEquals(defaultMinorGcSummary.getMin(), resetResultSummary.getMin());
    assertEquals(defaultMinorGcSummary.getMax(), resetResultSummary.getMax());

    assertEquals(defaultMajorGcSummary.getCount(), resetResultSummary.getCount());
    assertEquals(defaultMajorGcSummary.getSum(), resetResultSummary.getSum());
    assertEquals(defaultMajorGcSummary.getMin(), resetResultSummary.getMin());
    assertEquals(defaultMajorGcSummary.getMax(), resetResultSummary.getMax());
  }

  @Test
  void testUnsupportedGcEventName() {
    var event = mock(RecordedEvent.class);
    var eventStartTime = DEFAULT_START_TIME_MS + 1;
    var eventDurationNanos = 13_700_000;

    List<Metric> expected = List.of(defaultMinorGcSummary, defaultMajorGcSummary);
    var testClass = new BasicGarbageCollectionSummarizer(DEFAULT_START_TIME_MS);

    when(event.getValue("name")).thenReturn("FOO");
    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getDuration("duration")).thenReturn(Duration.ofNanos(eventDurationNanos));

    testClass.accept(event);
    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(expected, result);

    for (Summary summary : result) {
      var summaryName = summary.getName();
      if (summaryName.equals(MINOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(DEFAULT_COUNT, summary.getCount());
      } else if (summaryName.equals(MAJOR_GC_DURATION_METRIC_NAME)) {
        assertEquals(DEFAULT_COUNT, summary.getCount());
      }
    }
  }
}
