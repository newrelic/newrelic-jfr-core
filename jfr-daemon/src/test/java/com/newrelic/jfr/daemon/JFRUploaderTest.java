package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
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

    var testClass =
        new JFRUploader(telemetryClient, recordedEventBuffer, eventConverter, x -> recordingFile, deleter);

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
        new JFRUploader(telemetryClient, recordedEventBuffer, eventConverter, x -> recordingFile, deleter);

    assertThrows(RuntimeException.class, () -> testClass.handleFile(filePath));
    assertTrue(deleterCalled.get());
  }

  @Test
  void testBufferingThrowsExceptionIsHandled() throws Exception {
    doThrow(new OutOfMemoryError("Whoopsie doodle"))
            .when(recordedEventBuffer).bufferEvents(filePath, recordingFile);

    var testClass =
        new JFRUploader(telemetryClient, recordedEventBuffer, eventConverter, x -> recordingFile, deleter);

    testClass.handleFile(filePath);
    verifyNoInteractions(telemetryClient);
  }

  @Test
  public void testConvertThrowsExceptionIsHandled() throws Exception {
    fail("build me");
  }
}
