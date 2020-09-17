package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
  private MetricBatch expectedMetricBatch;
  private EventBatch expectedEventBatch;
  private RecordingFile recordingFile;
  private BufferedTelemetry bufferedTelemetry;
  private Consumer<Path> deleter;
  private EventConverter eventConverter;
  private RecordedEventBuffer recordedEventBuffer;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() throws Exception {
    now = Instant.now();
    filePath = Path.of("/foo", "bar", "baz");
    telemetryClient = mock(TelemetryClient.class);
    recordedEventBuffer = mock(RecordedEventBuffer.class);
    expectedMetricBatch = mock(MetricBatch.class);
    expectedEventBatch = mock(EventBatch.class);
    recordingFile = mock(RecordingFile.class);
    bufferedTelemetry = mock(BufferedTelemetry.class);
    eventConverter = mock(EventConverter.class);
    deleter = mock(Consumer.class);

    when(eventConverter.convert(recordedEventBuffer)).thenReturn(bufferedTelemetry);

    when(bufferedTelemetry.createEventBatch()).thenReturn(expectedEventBatch);
    when(bufferedTelemetry.createMetricBatch()).thenReturn(expectedMetricBatch);
  }

  @Test
  @Disabled
  void testUploads() {

    JFRUploader testClass = buildTestClass();

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
    JFRUploader testClass = buildTestClass();

    assertThrows(RuntimeException.class, () -> testClass.handleFile(filePath));
    assertTrue(deleterCalled.get());
  }

  @Test
  void testBufferingThrowsExceptionIsHandled() throws Exception {
    doThrow(new RuntimeException("Whoopsie doodle"))
        .when(recordedEventBuffer)
        .bufferEvents(filePath, recordingFile);

    JFRUploader testClass = buildTestClass();

    testClass.handleFile(filePath);
    // no exception, but we still try and send
    verify(telemetryClient).sendBatch(expectedMetricBatch);
    verify(telemetryClient).sendBatch(expectedEventBatch);
  }

  @Test
  void testSkipsIfNotReady() {
    JFRUploader testClass = buildTestClass(false);
    testClass.handleFile(filePath);
    verifyNoInteractions(eventConverter);
    verifyNoInteractions(telemetryClient);
  }

  private JFRUploader buildTestClass() {
    return buildTestClass(true);
  }

  private JFRUploader buildTestClass(boolean ready) {
    var testClass =
        JFRUploader.builder()
            .telemetryClient(telemetryClient)
            .recordedEventBuffer(recordedEventBuffer)
            .eventConverter(eventConverter)
            .recordingFileOpener(x -> recordingFile)
            .fileDeleter(deleter)
            .readinessCheck(new AtomicBoolean(ready))
            .build();
    return testClass;
  }

  @Test
  public void testConvertThrowsExceptionIsHandled() throws Exception {
    doThrow(new RuntimeException("kaboom!")).when(eventConverter).convert(recordedEventBuffer);

    var testClass =
        JFRUploader.builder()
            .telemetryClient(telemetryClient)
            .recordedEventBuffer(recordedEventBuffer)
            .eventConverter(eventConverter)
            .recordingFileOpener(x -> recordingFile)
            .fileDeleter(deleter)
            .build();

    testClass.handleFile(filePath);
    // no exception, and since we can't convert don't try sending
    verifyNoMoreInteractions(telemetryClient);
  }
}
