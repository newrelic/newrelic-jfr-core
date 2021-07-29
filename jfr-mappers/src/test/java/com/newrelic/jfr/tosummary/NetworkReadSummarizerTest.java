package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.tosummary.PerThreadNetworkReadSummarizer.BYTES_READ;
import static com.newrelic.jfr.tosummary.PerThreadNetworkReadSummarizer.JFR_SOCKET_READ_BYTES_READ;
import static com.newrelic.jfr.tosummary.PerThreadNetworkReadSummarizer.JFR_SOCKET_READ_DURATION;
import static com.newrelic.jfr.tosummary.PerThreadNetworkReadSummarizer.THREAD_NAME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class NetworkReadSummarizerTest {
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;
  private static ThreadNameNormalizer tnn;
  private static final String EVENT_THREAD = "eventThread";

  @BeforeAll
  static void init() {
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

    tnn = Mockito.mock(ThreadNameNormalizer.class);
  }

  @AfterAll
  static void teardown() {
    recordedObjectValidatorsMockedStatic.close();
  }

  @AfterEach
  public void resetMocks() {
    Mockito.reset(tnn);
  }

  @Test
  void testApplyWithThreadName() {
    var threadName1 = "spam";
    var threadName2 = "musubi";
    final Instant time1 = Instant.now();
    final Instant time2 = time1.plus(3, SECONDS);
    final Instant time3 = time2.plus(1, SECONDS);
    when(tnn.getNormalizedThreadName(any(String.class)))
        .thenAnswer(
            invocation -> {
              return invocation.getArgument(0, String.class);
            });

    var summary1bytes =
        new Summary(
            JFR_SOCKET_READ_BYTES_READ,
            2,
            13 + 17,
            13,
            17,
            time1.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, threadName1));
    var summary1duration =
        new Summary(
            JFR_SOCKET_READ_DURATION,
            2,
            Duration.between(time1, time3).toMillis(),
            Duration.between(time2, time3).toMillis(),
            Duration.between(time1, time2).toMillis(),
            time1.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, threadName1));
    var summary2bytes =
        new Summary(
            JFR_SOCKET_READ_BYTES_READ,
            1,
            12,
            12,
            12,
            time2.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, threadName2));
    var summary2duration =
        new Summary(
            JFR_SOCKET_READ_DURATION,
            1,
            Duration.between(time2, time3).toMillis(),
            Duration.between(time2, time3).toMillis(),
            Duration.between(time2, time3).toMillis(),
            time2.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, threadName2));
    List<Summary> expected =
        List.of(summary2bytes, summary2duration, summary1bytes, summary1duration);

    var event1 = buildEvent(threadName1, 13, time1, time2);
    var event2 = buildEvent(threadName2, 12, time2, time3);
    var event3 = buildEvent(threadName1, 17, time2, time3);

    NetworkReadSummarizer summarizer = new NetworkReadSummarizer(tnn);
    summarizer.accept(event1);
    summarizer.accept(event2);
    summarizer.accept(event3);

    var result = summarizer.summarize();

    assertEquals(expected, result.collect(toList()));
  }

  @Test
  void testApplyWithNormalizedThreadName() {
    var threadName1 = "thread1";
    var threadName2 = "thread2";
    var groupedThreadName = "thread#";
    when(tnn.getNormalizedThreadName(any(String.class))).thenReturn(groupedThreadName);

    final Instant time1 = Instant.now();
    final Instant time2 = time1.plus(3, SECONDS);
    final Instant time3 = time2.plus(1, SECONDS);

    var summary1bytes =
        new Summary(
            JFR_SOCKET_READ_BYTES_READ,
            3,
            13 + 12 + 17,
            12,
            17,
            time1.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, groupedThreadName));
    var summary1duration =
        new Summary(
            JFR_SOCKET_READ_DURATION,
            3,
            Duration.between(time1, time3).toMillis() // event1 + event2
                + Duration.between(time2, time3).toMillis(), // event3
            Duration.between(time2, time3).toMillis(),
            Duration.between(time1, time2).toMillis(),
            time1.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, groupedThreadName));
    List<Summary> expected = List.of(summary1bytes, summary1duration);

    var event1 = buildEvent(threadName1, 13, time1, time2);
    var event2 = buildEvent(threadName2, 12, time2, time3);
    var event3 = buildEvent(threadName1, 17, time2, time3);

    NetworkReadSummarizer summarizer = new NetworkReadSummarizer(tnn);
    summarizer.accept(event1);
    summarizer.accept(event2);
    summarizer.accept(event3);

    var result = summarizer.summarize();

    assertEquals(expected, result.collect(toList()));
  }

  @Test
  void testReset() {
    var threadName1 = "spam";
    final Instant time1 = Instant.now();
    final Instant time2 = time1.plus(3, SECONDS);

    when(tnn.getNormalizedThreadName(any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class));

    var event1 = buildEvent(threadName1, 13, time1, time2);

    NetworkReadSummarizer summarizer = new NetworkReadSummarizer(tnn);
    summarizer.accept(event1);

    var summaries = summarizer.summarize();

    assertEquals(2, summaries.collect(toList()).size());

    summarizer.reset();
    var emptySummaries = summarizer.summarize();
    assertEquals(0, emptySummaries.collect(toList()).size());
  }

  private RecordedEvent buildEvent(
      String threadName, long bytes, Instant startTime, Instant endTime) {
    var recordedThread = mock(RecordedThread.class);
    when(recordedThread.getJavaName()).thenReturn(threadName);

    var event = mock(RecordedEvent.class);
    when(event.getValue(EVENT_THREAD)).thenReturn(recordedThread);
    when(event.getLong(BYTES_READ)).thenReturn(bytes);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getDuration()).thenReturn(Duration.between(startTime, endTime));
    return event;
  }
}
