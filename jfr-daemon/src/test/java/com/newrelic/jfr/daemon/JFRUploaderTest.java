package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.daemon.buffer.BufferedTelemetry;
import com.newrelic.telemetry.TelemetryClient;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.MetricBatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JFRUploaderTest {

  @Mock TelemetryClient client;
  @Mock BufferedTelemetry buffer;
  @Mock MetricBatch metricBatch;
  @Mock EventBatch eventBatch;

  @Test
  void testSendHappyPath() {
    var testClass = new JFRUploader(client);
    when(buffer.createMetricBatch()).thenReturn(metricBatch);
    when(buffer.createEventBatch()).thenReturn(eventBatch);
    when(metricBatch.isEmpty()).thenReturn(false);
    when(eventBatch.isEmpty()).thenReturn(false);
    testClass.send(buffer);
    verify(client).sendBatch(metricBatch);
    verify(client).sendBatch(eventBatch);
  }

  @Test
  void testEmptyEventBatch() {
    var testClass = new JFRUploader(client);
    when(buffer.createMetricBatch()).thenReturn(metricBatch);
    when(buffer.createEventBatch()).thenReturn(eventBatch);
    when(eventBatch.isEmpty()).thenReturn(true);
    testClass.send(buffer);
    verify(client, never()).sendBatch(eventBatch);
  }

  @Test
  void testEmptyMetricBatch() {
    var testClass = new JFRUploader(client);
    when(buffer.createMetricBatch()).thenReturn(metricBatch);
    when(buffer.createEventBatch()).thenReturn(eventBatch);
    when(metricBatch.isEmpty()).thenReturn(true);
    testClass.send(buffer);
    verify(client, never()).sendBatch(metricBatch);
  }
}
