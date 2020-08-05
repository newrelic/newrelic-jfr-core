package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.daemon.buffer.BufferedTelemetry;
import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RawEventConsumerTaskTest {

  @Mock RecordedEvent event;
  @Mock BufferedTelemetry bufferedTelemetry;
  @Mock RecordedEventToTelemetry recordedEventToTelemetry;
  @Mock LastSendStateTracker lastSendStateTracker;
  @Mock ToSummaryRegistry toSummary;
  @Mock JFRUploader uploader;
  @Mock EventToSummary summarizer;

  @Test
  void testHappyPathForSingleEvent() throws Exception {
    var queue = new ArrayBlockingQueue<RecordedEvent>(10);
    var summary1 = new Summary("foo", 12, 101, 102, 103, 123456, 123457, new Attributes());
    var summary2 = new Summary("bar", 13, 201, 202, 203, 123458, 123459, new Attributes());
    var latch = new CountDownLatch(1);

    when(bufferedTelemetry.getTotalSize()).thenReturn(99);
    when(lastSendStateTracker.isReady(99)).thenReturn(true);
    when(toSummary.all()).thenReturn(Stream.of(summarizer));
    when(summarizer.summarizeAndReset()).thenReturn(Stream.of(summary1, summary2));

    doAnswer(
            x -> {
              verify(bufferedTelemetry).addMetric(summary1);
              verify(bufferedTelemetry).addMetric(summary2);
              latch.countDown();
              return null;
            })
        .when(uploader)
        .send(bufferedTelemetry);

    queue.put(event);

    var testClass =
        RawEventConsumerTask.builder()
            .rawEventQueue(queue)
            .bufferedTelemetry(bufferedTelemetry)
            .lastSendStateTracker(lastSendStateTracker)
            .recordedEventToTelemetry(recordedEventToTelemetry)
            .toSummaryRegistry(toSummary)
            .uploader(uploader)
            .build();
    var executorService = Executors.newSingleThreadExecutor();
    executorService.submit(testClass);
    assertTrue(latch.await(2, TimeUnit.SECONDS));

    testClass.shutdown();
    verify(recordedEventToTelemetry).processEvent(event, bufferedTelemetry);
    verify(lastSendStateTracker).updateSendTime();
  }

  @Test
  void testShutdownBeforeDone() {
    var eventQueue = mock(BlockingQueue.class);
    var testClass = RawEventConsumerTask.builder().rawEventQueue(eventQueue).build();

    testClass.shutdown();
    testClass.run();
    verifyNoInteractions(eventQueue);
  }

  @Test
  void testQueueExceptionHandledGracefully() throws Exception {
    var queue = new ArrayBlockingQueue<RecordedEvent>(10);
    var exitedCleanly = new AtomicBoolean(false);
    var testClass =
        RawEventConsumerTask.builder()
            .rawEventQueue(queue)
            .lastSendStateTracker(lastSendStateTracker)
            .bufferedTelemetry(bufferedTelemetry)
            .build();
    var executorService = Executors.newSingleThreadExecutor();
    executorService.submit(
        () -> {
          testClass.run();
          exitedCleanly.set(true);
        });
    // Shutdown now forces an interrupt
    executorService.shutdownNow();
    assertTrue(executorService.awaitTermination(22222, TimeUnit.SECONDS));
    assertTrue(testClass.isShutdown());
    assertTrue(exitedCleanly.get());
  }
}
