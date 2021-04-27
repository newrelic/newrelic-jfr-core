package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.tosummary.PerThreadObjectAllocationOutsideTLABSummarizer.ALLOCATION_SIZE;
import static com.newrelic.jfr.tosummary.PerThreadObjectAllocationOutsideTLABSummarizer.JFR_OBJECT_ALLOCATION_OUTSIDE_TLAB_ALLOCATION;
import static com.newrelic.jfr.tosummary.PerThreadObjectAllocationOutsideTLABSummarizer.THREAD_NAME;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

class ObjectAllocationOutsideTLABSummarizerTest {
  private static final String EVENT_THREAD = "eventThread";

  @Test
  void testSingleEventSummary() {
    var recordedThread = mock(RecordedThread.class);
    var eventThreadName = "main";

    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = Instant.now().toEpochMilli();
    var eventAllocationSize = 1500L;
    var attr = new Attributes().put(THREAD_NAME, eventThreadName);

    var expectedSummaryMetric =
        new Summary(
            JFR_OBJECT_ALLOCATION_OUTSIDE_TLAB_ALLOCATION,
            numOfEvents, // count
            eventAllocationSize, // sum
            eventAllocationSize, // min
            eventAllocationSize, // max
            eventStartTime, // startTimeMs: the summary metric startTimeMs is the eventStartTime of
            // the initial RecordedEvent
            eventStartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of the
            // last RecordedEvent
            attr); // attributes contain threadName

    List<Metric> expected = List.of(expectedSummaryMetric);
    var testClass = new ObjectAllocationOutsideTLABSummarizer();

    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getValue(EVENT_THREAD)).thenReturn(recordedThread);
    when(event.getLong(ALLOCATION_SIZE)).thenReturn(eventAllocationSize);

    when(recordedThread.getJavaName()).thenReturn(eventThreadName);

    testClass.accept(event);

    final List<Summary> result = testClass.summarize().collect(toList());
    final Summary resetResultSummary = testClass.summarize().collect(toList()).get(0);

    assertEquals(expected, result);
  }

  @Test
  void testMultipleEventSummary() {
    var recordedThread = mock(RecordedThread.class);
    var eventThreadName = "main";

    var event1 = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var event1StartTime = Instant.now().toEpochMilli();
    var event1AllocationSize = 847L;
    var attr = new Attributes().put(THREAD_NAME, eventThreadName);

    var event2 = mock(RecordedEvent.class);
    numOfEvents = ++numOfEvents;
    var event2StartTime = event1StartTime + 1;
    var event2AllocationSize = 520L; // min allocation size of final summary

    var event3 = mock(RecordedEvent.class);
    numOfEvents = ++numOfEvents;
    var event3StartTime = event1StartTime + 2;
    var event3AllocationSize = 1760L; // max allocation size of final summary

    var summedAllocationSize = event1AllocationSize + event2AllocationSize + event3AllocationSize;

    var expectedSummaryMetric =
        new Summary(
            JFR_OBJECT_ALLOCATION_OUTSIDE_TLAB_ALLOCATION,
            numOfEvents, // count
            summedAllocationSize, // sum
            event2AllocationSize, // min
            event3AllocationSize, // max
            event1StartTime, // startTimeMs: the summary metric startTimeMs is the eventStartTime of
            // the initial RecordedEvent
            event3StartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of the
            // last RecordedEvent
            attr); // attributes contain threadName

    List<Metric> expected = List.of(expectedSummaryMetric);
    var testClass = new ObjectAllocationOutsideTLABSummarizer();

    when(event1.getStartTime()).thenReturn(Instant.ofEpochMilli(event1StartTime));
    when(event1.getLong(ALLOCATION_SIZE)).thenReturn(event1AllocationSize);
    when(event1.getValue(EVENT_THREAD)).thenReturn(recordedThread);

    when(event2.getStartTime()).thenReturn(Instant.ofEpochMilli(event2StartTime));
    when(event2.getLong(ALLOCATION_SIZE)).thenReturn(event2AllocationSize);
    when(event2.getValue(EVENT_THREAD)).thenReturn(recordedThread);

    when(event3.getStartTime()).thenReturn(Instant.ofEpochMilli(event3StartTime));
    when(event3.getLong(ALLOCATION_SIZE)).thenReturn(event3AllocationSize);
    when(event3.getValue(EVENT_THREAD)).thenReturn(recordedThread);

    when(recordedThread.getJavaName()).thenReturn(eventThreadName);

    // Summarize all events
    testClass.accept(event1);
    testClass.accept(event2);
    testClass.accept(event3);

    final List<Summary> result = testClass.summarize().collect(toList());
    final Summary resetResultSummary = testClass.summarize().collect(toList()).get(0);

    assertEquals(expected, result);
  }

  @Test
  void testReset() {
    var recordedThread = mock(RecordedThread.class);
    var eventThreadName = "main";

    var event = mock(RecordedEvent.class);
    var eventStartTime = Instant.now().toEpochMilli();
    var eventAllocationSize = 1500L;

    var testClass = new ObjectAllocationOutsideTLABSummarizer();

    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getValue(EVENT_THREAD)).thenReturn(recordedThread);
    when(event.getLong(ALLOCATION_SIZE)).thenReturn(eventAllocationSize);

    when(recordedThread.getJavaName()).thenReturn(eventThreadName);

    testClass.accept(event);

    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(1, result.size());

    testClass.reset();
    final List<Summary> summarizers = testClass.summarize().collect(toList());
    assertTrue(summarizers.isEmpty());
  }
}
