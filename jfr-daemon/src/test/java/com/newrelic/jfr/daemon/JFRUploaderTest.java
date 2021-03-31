package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.MetricBatch;
import java.nio.file.Path;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JFRUploaderTest {

  private Path filePath;
  private TelemetryClient telemetryClient;
  private MetricBatch expectedMetricBatch;
  private EventBatch expectedEventBatch;
  private RecordingFile recordingFile;
  private EventConverter eventConverter;
  private RecordedEventBuffer recordedEventBuffer;

  private JFRUploader testClass;

  @BeforeEach
  void setup() {
    filePath = Path.of("/foo", "bar", "baz");
    telemetryClient = mock(TelemetryClient.class);
    recordedEventBuffer = mock(RecordedEventBuffer.class);
    expectedMetricBatch = mock(MetricBatch.class);
    expectedEventBatch = mock(EventBatch.class);
    recordingFile = mock(RecordingFile.class);
    eventConverter = mock(EventConverter.class);
    var bufferedTelemetry = mock(BufferedTelemetry.class);

    when(eventConverter.convert(recordedEventBuffer)).thenReturn(bufferedTelemetry);
    when(bufferedTelemetry.createEventBatch()).thenReturn(expectedEventBatch);
    when(bufferedTelemetry.createMetricBatch()).thenReturn(expectedMetricBatch);

    testClass =
        spy(new JFRUploader(new NewRelicTelemetrySender(telemetryClient), recordedEventBuffer));
    testClass.readyToSend(eventConverter);

    doReturn(recordingFile).when(testClass).openRecordingFile(filePath);
    doNothing().when(testClass).deleteFile(any());
  }

  @Test
  void testUploads() {
    testClass.handleFile(filePath);

    verify(telemetryClient).sendBatch(expectedMetricBatch);
    verify(telemetryClient).sendBatch(expectedEventBatch);
    verify(testClass).deleteFile(filePath);
  }

  @Test
  void testDeleteFails() {
    doThrow(new RuntimeException("KABOOM!")).when(testClass).deleteFile(filePath);

    assertThrows(RuntimeException.class, () -> testClass.handleFile(filePath));
    verify(testClass).deleteFile(filePath);
  }

  @Test
  public void testParentAlsoDeleted() {
    testClass.handleFile(filePath);
    verify(testClass).deleteFile(filePath);
    verify(testClass).deleteFile(filePath.getParent());
  }

  @Test
  void testBufferingThrowsExceptionIsHandled() throws Exception {
    doThrow(new RuntimeException("Whoopsie doodle"))
        .when(recordedEventBuffer)
        .bufferEvents(filePath, recordingFile);

    testClass.handleFile(filePath);
    // no exception, but we still try and send
    verify(telemetryClient).sendBatch(expectedMetricBatch);
    verify(telemetryClient).sendBatch(expectedEventBatch);
  }

  @Test
  public void testConvertThrowsExceptionIsHandled() {
    doThrow(new RuntimeException("kaboom!")).when(eventConverter).convert(recordedEventBuffer);

    testClass.handleFile(filePath);
    // no exception, and since we can't convert don't try sending
    verifyNoMoreInteractions(telemetryClient);
  }
}
