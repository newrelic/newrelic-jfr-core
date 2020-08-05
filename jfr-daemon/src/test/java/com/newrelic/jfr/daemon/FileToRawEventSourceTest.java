package com.newrelic.jfr.daemon;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileToRawEventSourceTest {

  @Mock RecordingFile recordingFile;

  @Mock RecordedEvent event;
  @Mock EventType eventType;

  @Test
  void testHappyPathQueue() throws Exception {
    String filename = "somefile.jfr";
    Instant after = Instant.ofEpochMilli(123123123);

    var consumer = mock(Consumer.class);
    when(recordingFile.hasMoreEvents()).thenReturn(true, false);
    when(recordingFile.readEvent()).thenReturn(event);
    when(event.getEventType()).thenReturn(eventType);
    when(event.getStartTime()).thenReturn(after.plus(30, ChronoUnit.SECONDS));

    var testClass = new FileToRawEventSource(consumer);
    testClass.queueRawEvents(recordingFile, after, filename);
    verify(consumer).accept(event);
  }

  @Test
  void testEventTooOld() throws Exception {
    String filename = "somefile.jfr";
    Instant after = Instant.ofEpochMilli(123123123);

    var consumer = mock(Consumer.class);

    when(recordingFile.hasMoreEvents()).thenReturn(true, false);
    when(recordingFile.readEvent()).thenReturn(event);
    when(event.getStartTime()).thenReturn(after.minus(30, ChronoUnit.SECONDS));

    var testClass = new FileToRawEventSource(consumer);
    testClass.queueRawEvents(recordingFile, after, filename);
    verify(consumer, never()).accept(event);
  }

  @Test
  void testNullEvent() throws Exception {
    String filename = "somefile.jfr";
    Instant after = Instant.ofEpochMilli(123123123);

    var consumer = mock(Consumer.class);

    when(recordingFile.hasMoreEvents()).thenReturn(true, false);
    when(recordingFile.readEvent()).thenReturn(null);

    var testClass = new FileToRawEventSource(consumer);
    testClass.queueRawEvents(recordingFile, after, filename);
    verify(consumer, never()).accept(event);
  }
}
