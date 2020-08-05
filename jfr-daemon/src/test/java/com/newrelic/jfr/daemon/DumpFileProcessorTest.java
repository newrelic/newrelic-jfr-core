package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DumpFileProcessorTest {

  Instant now;
  Path filePath;
  Function<Path, RecordingFile> opener;

  @Mock FileToRawEventSource fileToRawEvents;
  @Mock RecordingFile recordingFile;
  @Mock Consumer<Path> deleter;

  @BeforeEach
  void setup() {
    now = Instant.now();
    filePath = Path.of("/foo", "bar", "baz");
    opener =
        p -> {
          assertEquals(filePath, p);
          return recordingFile;
        };
  }

  @Test
  void handleTwoFilesHappyPath() throws Exception {
    var dumpFilename = filePath.toString();
    var after = Instant.ofEpochMilli(123123123);
    var lastSeen = after.plus(5, ChronoUnit.SECONDS);
    var expectedLastSeen = lastSeen.plus(5, ChronoUnit.SECONDS);

    when(fileToRawEvents.queueRawEvents(recordingFile, Instant.EPOCH, dumpFilename))
        .thenReturn(lastSeen);
    when(fileToRawEvents.queueRawEvents(recordingFile, lastSeen, dumpFilename))
        .thenReturn(expectedLastSeen);

    var testClass = new DumpFileProcessor(fileToRawEvents, opener, deleter);
    assertTrue(testClass.getLastSeen().isEmpty());
    testClass.handleFile(filePath);
    testClass.handleFile(filePath);
    assertEquals(expectedLastSeen, testClass.getLastSeen().get());
    verify(deleter, times(2)).accept(filePath);
  }

  @Test
  void testQueueingEventsThrows() throws Exception {
    var dumpFilename = filePath.toString();

    when(fileToRawEvents.queueRawEvents(recordingFile, Instant.EPOCH, dumpFilename))
        .thenThrow(new RuntimeException("Kaboom"));

    var testClass = new DumpFileProcessor(fileToRawEvents, opener, deleter);

    testClass.handleFile(filePath);
    assertTrue(testClass.getLastSeen().isEmpty()); // never got set due to boom
    verify(deleter).accept(filePath);
  }

  @Test
  void testDeleteFileTrapsException() throws Exception {
    var dumpFilename = filePath.toString();
    var after = Instant.ofEpochMilli(123123123);
    var lastSeen = after.plus(5, ChronoUnit.SECONDS);

    doThrow(new RuntimeException("can't delete")).when(deleter).accept(filePath);
    when(fileToRawEvents.queueRawEvents(recordingFile, Instant.EPOCH, dumpFilename))
        .thenReturn(lastSeen);

    var testClass = new DumpFileProcessor(fileToRawEvents, opener, deleter);

    testClass.handleFile(filePath);
    assertEquals(lastSeen, testClass.getLastSeen().get());
  }
}
