package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.Workarounds.EVENT_THREAD;
import static com.newrelic.jfr.tosummary.PerThreadNetworkWriteSummarizer.BYTES_WRITTEN;
import static com.newrelic.jfr.tosummary.PerThreadNetworkWriteSummarizer.JFR_SOCKET_WRITE_BYTES_WRITTEN;
import static com.newrelic.jfr.tosummary.PerThreadNetworkWriteSummarizer.JFR_SOCKET_WRITE_DURATION;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.BasicThreadInfo;
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

class NetworkWriteSummarizerTest {
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;
  private static ThreadNameNormalizer tnn;
  private static final String THREAD_NAME = "thread.name";

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
  void testApply() {
    var threadName1 = "spam";
    var threadName2 = "musubi";
    when(tnn.getNormalizedThreadName(any(BasicThreadInfo.class)))
        .thenAnswer(
            invocation -> {
              BasicThreadInfo threadInfo = invocation.getArgument(0, BasicThreadInfo.class);
              return threadInfo.getName();
            });

    var time1 = Instant.now();
    var time2 = time1.plus(3, SECONDS);
    var time3 = time2.plus(1, SECONDS);

    var summary1bytes =
        new Summary(
            JFR_SOCKET_WRITE_BYTES_WRITTEN,
            2,
            13 + 17,
            13,
            17,
            time1.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, threadName1));
    var summary1duration =
        new Summary(
            JFR_SOCKET_WRITE_DURATION,
            2,
            Duration.between(time1, time3).toMillis(),
            Duration.between(time2, time3).toMillis(),
            Duration.between(time1, time2).toMillis(),
            time1.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, threadName1));
    var summary2bytes =
        new Summary(
            JFR_SOCKET_WRITE_BYTES_WRITTEN,
            1,
            12,
            12,
            12,
            time2.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, threadName2));
    var summary2duration =
        new Summary(
            JFR_SOCKET_WRITE_DURATION,
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

    NetworkWriteSummarizer summarizer = new NetworkWriteSummarizer(tnn);
    summarizer.accept(event1);
    summarizer.accept(event2);
    summarizer.accept(event3);

    var result = summarizer.summarize();

    assertEquals(expected, result.collect(toList()));
  }

  @Test
  void testApplyWithThreadNameNormalizer() {
    var threadName1 = "thread1";
    var threadName2 = "thread2";
    var groupedThreadName = "thread#";
    when(tnn.getNormalizedThreadName(any(BasicThreadInfo.class))).thenReturn(groupedThreadName);

    var time1 = Instant.now();
    var time2 = time1.plus(3, SECONDS);
    var time3 = time2.plus(1, SECONDS);

    var summary1bytes =
        new Summary(
            JFR_SOCKET_WRITE_BYTES_WRITTEN,
            3,
            13 + 12 + 17,
            12,
            17,
            time1.toEpochMilli(),
            time3.toEpochMilli(),
            new Attributes().put(THREAD_NAME, groupedThreadName));
    var summary1duration =
        new Summary(
            JFR_SOCKET_WRITE_DURATION,
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

    NetworkWriteSummarizer summarizer = new NetworkWriteSummarizer(tnn);
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

    var event1 = buildEvent(threadName1, 13, time1, time2);

    when(tnn.getNormalizedThreadName(any(BasicThreadInfo.class)))
        .thenAnswer(
            invocation -> {
              BasicThreadInfo threadInfo = invocation.getArgument(0, BasicThreadInfo.class);
              return threadInfo.getName();
            });

    NetworkWriteSummarizer summarizer = new NetworkWriteSummarizer(tnn);
    summarizer.accept(event1);

    var summaries = summarizer.summarize();

    assertEquals(2, (int) summaries.count());

    summarizer.reset();
    var emptySummaries = summarizer.summarize();
    assertEquals(0, (int) emptySummaries.count());
  }

  private RecordedEvent buildEvent(
      String threadName, long bytes, Instant startTime, Instant endTime) {
    var recordedThread = mock(RecordedThread.class);
    when(recordedThread.getJavaName()).thenReturn(threadName);
    var event = mock(RecordedEvent.class);
    when(event.getValue(EVENT_THREAD)).thenReturn(recordedThread);
    when(event.getLong(BYTES_WRITTEN)).thenReturn(bytes);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getDuration()).thenReturn(Duration.between(startTime, endTime));
    return event;
  }
}
