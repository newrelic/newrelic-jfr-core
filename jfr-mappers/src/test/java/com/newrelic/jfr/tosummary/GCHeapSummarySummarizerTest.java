package com.newrelic.jfr.tosummary;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GCHeapSummarySummarizerTest {

  private static Summary defaultSummary;

  @BeforeAll
  static void init() {
    defaultSummary =
        new Summary(
            "jfr.ObjectAllocationInNewTLAB.allocation",
            0,
            Duration.ofNanos(0L).toMillis(),
            Duration.ofNanos(Long.MAX_VALUE).toMillis(),
            Duration.ofNanos(Long.MIN_VALUE).toMillis(),
            Instant.now().toEpochMilli(),
            0L,
            new Attributes());
  }

  @Test
  void mock_pairEventSummaryAndReset() {
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
    var afterStartTime = summaryStartTime + 11;
    var afterDurationNanos = 24800000; // max duration of final summary
    var afterDurationMillis = Duration.ofNanos(afterDurationNanos).toMillis();

    when(after.getString("when")).thenReturn("After GC");
    when(after.getStartTime()).thenReturn(Instant.ofEpochMilli(afterStartTime));
    when(after.getDuration("duration")).thenReturn(Duration.ofNanos(afterDurationNanos));

    var pairDurationMillis = afterStartTime - beforeStartTime;

    var expectedSummaryMetric =
        new Summary(
            "jfr.GarbageCollection.duration",
            numOfEvents, // count
            pairDurationMillis, // sum
            pairDurationMillis, // min
            pairDurationMillis, // max
            summaryStartTime, // startTimeMs
            afterStartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of
            // each RecordedEvent
            new Attributes());

    var expected = List.of(expectedSummaryMetric);

    var testClass = new GCHeapSummarySummarizer(summaryStartTime);

    // Summarize all events
    testClass.accept(before);
    testClass.accept(after);

    var result = testClass.summarize().collect(toList());
    testClass.reset();
    assertEquals(expected, result);

    var resetResultSummary = testClass.summarize().collect(toList()).get(0);

    // Summary should be reset to default values
    testClass.reset();
    assertEquals(defaultSummary.getCount(), resetResultSummary.getCount());
    assertEquals(defaultSummary.getSum(), resetResultSummary.getSum());
    assertEquals(defaultSummary.getMin(), resetResultSummary.getMin());
    assertEquals(defaultSummary.getMax(), resetResultSummary.getMax());
  }

  @Test
  void read_real_event() throws Exception {
    final var dumpFile =
        Path.of("src", "test", "resources", "hotspot-pid-241-2020_10_15_12_02_23.jfr");

    var testClass = new GCHeapSummarySummarizer();

    try (final var recordingFile = new RecordingFile(dumpFile)) {
      while (recordingFile.hasMoreEvents()) {
        var event = recordingFile.readEvent();

        if (event != null) {
          if (event.getEventType().getName().equals(GCHeapSummarySummarizer.EVENT_NAME)) {
            testClass.accept(event);
          }
        }
      }
    }
    var resetResultSummary = testClass.summarize().collect(toList()).get(0);

    // Summary should be reset to default values
    testClass.reset();

    assertEquals(2, resetResultSummary.getCount());
    assertEquals(109.0, resetResultSummary.getSum());
    assertEquals(109.0, resetResultSummary.getMin());
    assertEquals(109.0, resetResultSummary.getMax());
  }

  @Test
  void read_multiple_real_events() throws Exception {
    final var dumpFile = Path.of("src", "test", "resources", "startup3.jfr");

    var testClass = new GCHeapSummarySummarizer();

    try (final var recordingFile = new RecordingFile(dumpFile)) {
      while (recordingFile.hasMoreEvents()) {
        var event = recordingFile.readEvent();

        if (event != null) {
          if (event.getEventType().getName().equals(GCHeapSummarySummarizer.EVENT_NAME)) {
            testClass.accept(event);
          }
        }
      }
    }
    var resetResultSummary = testClass.summarize().collect(toList()).get(0);

    // Summary should be reset to default values
    testClass.reset();

    assertEquals(350, resetResultSummary.getCount());
    assertEquals(105.0, resetResultSummary.getSum());
    assertEquals(0.0, resetResultSummary.getMin());
    assertEquals(3.0, resetResultSummary.getMax());
  }
}
