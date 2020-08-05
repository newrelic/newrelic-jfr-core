package com.newrelic.jfr.daemon;

import com.newrelic.jfr.daemon.buffer.BufferedTelemetry;
import com.newrelic.telemetry.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRUploader {

  private static final Logger logger = LoggerFactory.getLogger(JFRUploader.class);

  private final TelemetryClient telemetryClient;

  public JFRUploader(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
  }

  public void send(BufferedTelemetry telemetry) {
    sendMetrics(telemetry);
    sendEvents(telemetry);
  }

  private void sendMetrics(BufferedTelemetry bufferedMetrics) {
    var metricBatch = bufferedMetrics.createMetricBatch();
    if (!metricBatch.isEmpty()) {
      logger.info(String.format("Sending metric batch of size %s", metricBatch.size()));
      telemetryClient.sendBatch(metricBatch);
    }
  }

  private void sendEvents(BufferedTelemetry bufferedMetrics) {
    var eventBatch = bufferedMetrics.createEventBatch();
    if (!eventBatch.isEmpty()) {
      logger.info(String.format("Sending events batch of size %s", eventBatch.size()));
      telemetryClient.sendBatch(eventBatch);
    }
  }
}
