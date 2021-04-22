package com.newrelic.jfr.tosummary;

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
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class PerThreadObjectAllocationInNewTLABSummarizerTest {
  private static Summary defaultSummary;
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;

  @BeforeAll
  static void init() {
    defaultSummary =
        new Summary(
            "jfr.ObjectAllocationInNewTLAB.allocation",
            0,
            0L,
            Long.MAX_VALUE,
            0L,
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
    var recordedThread = mock(RecordedThread.class);
    var eventThreadName = "main";

    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = Instant.now().toEpochMilli();
    var eventTlabSize = 847L;
    var attr = new Attributes().put("thread.name", eventThreadName);

    var expectedSummaryMetric =
        new Summary(
            "jfr.ObjectAllocationInNewTLAB.allocation",
            numOfEvents, // count
            eventTlabSize, // sum
            eventTlabSize, // min
            eventTlabSize, // max
            eventStartTime, // startTimeMs: the summary metric startTimeMs is the eventStartTime of
            // the initial RecordedEvent
            eventStartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of the
            // last RecordedEvent
            attr); // attributes contain threadName

    List<Metric> expected = List.of(expectedSummaryMetric);
    var testClass =
        new PerThreadObjectAllocationInNewTLABSummarizer(eventThreadName, eventStartTime);

    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getValue("eventThread")).thenReturn(recordedThread);
    when(event.getLong("tlabSize")).thenReturn(eventTlabSize);

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
    var event1TlabSize = 847L;
    var attr = new Attributes().put("thread.name", eventThreadName);

    var event2 = mock(RecordedEvent.class);
    numOfEvents = ++numOfEvents;
    var event2StartTime = event1StartTime + 1;
    var event2TlabSize = 520L; // min TLAB size of final summary

    var event3 = mock(RecordedEvent.class);
    numOfEvents = ++numOfEvents;
    var event3StartTime = event1StartTime + 2;
    var event3TlabSize = 1760L; // max TLAB size of final summary

    var summedTlabSize = event1TlabSize + event2TlabSize + event3TlabSize;

    var expectedSummaryMetric =
        new Summary(
            "jfr.ObjectAllocationInNewTLAB.allocation",
            numOfEvents, // count
            summedTlabSize, // sum
            event2TlabSize, // min
            event3TlabSize, // max
            event1StartTime, // startTimeMs: the summary metric startTimeMs is the eventStartTime of
            // the initial RecordedEvent
            event3StartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of the
            // last RecordedEvent
            attr); // attributes contain threadName

    List<Metric> expected = List.of(expectedSummaryMetric);
    var testClass =
        new PerThreadObjectAllocationInNewTLABSummarizer(eventThreadName, event1StartTime);

    when(event1.getStartTime()).thenReturn(Instant.ofEpochMilli(event1StartTime));
    when(event1.getLong("tlabSize")).thenReturn(event1TlabSize);
    when(event1.getValue("eventThread")).thenReturn(recordedThread);

    when(event2.getStartTime()).thenReturn(Instant.ofEpochMilli(event2StartTime));
    when(event2.getLong("tlabSize")).thenReturn(event2TlabSize);
    when(event2.getValue("eventThread")).thenReturn(recordedThread);

    when(event3.getStartTime()).thenReturn(Instant.ofEpochMilli(event3StartTime));
    when(event3.getLong("tlabSize")).thenReturn(event3TlabSize);
    when(event3.getValue("eventThread")).thenReturn(recordedThread);

    when(recordedThread.getJavaName()).thenReturn(eventThreadName);

    // Summarize all events
    testClass.accept(event1);
    testClass.accept(event2);
    testClass.accept(event3);

    final List<Summary> result = testClass.summarize().collect(toList());
    assertEquals(expected, result);
  }

  @Test
  public void testReset() {
    var recordedThread = mock(RecordedThread.class);
    var eventThreadName = "main";

    var event = mock(RecordedEvent.class);
    var numOfEvents = 1;
    var eventStartTime = Instant.now().toEpochMilli();
    var eventTlabSize = 847L;
    var attr = new Attributes().put("thread.name", eventThreadName);

    var expectedSummaryMetric =
        new Summary(
            "jfr.ObjectAllocationInNewTLAB.allocation",
            numOfEvents, // count
            eventTlabSize, // sum
            eventTlabSize, // min
            eventTlabSize, // max
            eventStartTime, // startTimeMs: the summary metric startTimeMs is the eventStartTime of
            // the initial RecordedEvent
            eventStartTime, // endTimeMs: the summary metric endTimeMs is the eventStartTime of the
            // last RecordedEvent
            attr); // attributes contain threadName

    List<Metric> expected = List.of(expectedSummaryMetric);
    var testClass =
        new PerThreadObjectAllocationInNewTLABSummarizer(eventThreadName, eventStartTime);

    when(event.getStartTime()).thenReturn(Instant.ofEpochMilli(eventStartTime));
    when(event.getValue("eventThread")).thenReturn(recordedThread);
    when(event.getLong("tlabSize")).thenReturn(eventTlabSize);

    when(recordedThread.getJavaName()).thenReturn(eventThreadName);

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
