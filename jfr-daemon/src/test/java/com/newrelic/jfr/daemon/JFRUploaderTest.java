package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.MetricBatch;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class JFRUploaderTest {

  private Instant now;;
  private Path filePath;
  private TelemetryClient telemetryClient;
  private FileToBufferedTelemetry fileBufferer;
  private MetricBatch expectedMetricBatch;
  private EventBatch expectedEventBatch;
  private RecordingFile recordingFile;
  private BufferedTelemetry bufferedTelemetry;
  private Consumer<Path> deleter;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() throws Exception {
    now = Instant.now();
    filePath = Path.of("/foo", "bar", "baz");
    telemetryClient = mock(TelemetryClient.class);
    fileBufferer = mock(FileToBufferedTelemetry.class);
    expectedMetricBatch = mock(MetricBatch.class);
    expectedEventBatch = mock(EventBatch.class);
    recordingFile = mock(RecordingFile.class);
    bufferedTelemetry = mock(BufferedTelemetry.class);
    deleter = mock(Consumer.class);

    FileToBufferedTelemetry.Result bufferResult = mock(FileToBufferedTelemetry.Result.class);
    when(fileBufferer.convert(recordingFile, now, filePath.toString())).thenReturn(bufferResult);
    when(bufferResult.getBufferedTelemetry()).thenReturn(bufferedTelemetry);
    when(bufferedTelemetry.createEventBatch()).thenReturn(expectedEventBatch);
    when(bufferedTelemetry.createMetricBatch()).thenReturn(expectedMetricBatch);
  }

  @Test
  @Disabled
  void testUploads() {

    var testClass =
        new JFRUploader(telemetryClient, fileBufferer, () -> now, x -> recordingFile, deleter);

    testClass.handleFile(filePath);

    verify(telemetryClient).sendBatch(expectedMetricBatch);
    verify(telemetryClient).sendBatch(expectedEventBatch);
    verify(deleter).accept(filePath);
  }

  @Test
  void testDeleteFails() {
    var deleterCalled = new AtomicBoolean(false);
    deleter =
        o -> {
          deleterCalled.set(true);
          throw new RuntimeException("KABOOM!");
        };
    var testClass =
        new JFRUploader(telemetryClient, fileBufferer, () -> now, x -> recordingFile, deleter);

    assertThrows(RuntimeException.class, () -> testClass.handleFile(filePath));
    assertTrue(deleterCalled.get());
  }

  @Test
  void testBufferingThrowsExceptionIsHandled() throws Exception {
    when(fileBufferer.convert(recordingFile, now, filePath.toString()))
        .thenThrow(new OutOfMemoryError("Whoopsie doodle"));

    var testClass =
        new JFRUploader(telemetryClient, fileBufferer, () -> now, x -> recordingFile, deleter);

    testClass.handleFile(filePath);
    verifyNoInteractions(telemetryClient);
  }
}
