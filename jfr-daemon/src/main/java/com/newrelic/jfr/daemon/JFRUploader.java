/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.TelemetryClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JFRUploader {
  private static final Logger logger = LoggerFactory.getLogger(JFRUploader.class);

  private final TelemetryClient telemetryClient;
  private final RecordedEventBuffer eventBuffer;
  private final EventConverter eventConverter;

  public JFRUploader(
      TelemetryClient telemetryClient,
      RecordedEventBuffer eventBuffer,
      EventConverter eventConverter) {
    this.telemetryClient = telemetryClient;
    this.eventBuffer = eventBuffer;
    this.eventConverter = eventConverter;
  }

  /**
   * Handle the JFR {@code dumpFile}. Buffer new events, then convert them and them to New Relic via
   * {@link #telemetryClient}. Finally, delete the file and its parent directory.
   *
   * @param dumpFile the JFR file
   */
  public void handleFile(final Path dumpFile) {
    try {
      bufferFileData(dumpFile);
      drainAndSend();
    } catch (Exception e) {
      logger.error("Error handling raw dump file", e);
    } finally {
      deleteFile(dumpFile);
      deleteFile(dumpFile.getParent());
    }
  }

  private void bufferFileData(Path dumpFile) {
    try (var recordingFile = openRecordingFile(dumpFile)) {
      eventBuffer.bufferEvents(dumpFile, recordingFile);
    } catch (Throwable t) {
      logger.error("Error processing file " + dumpFile, t);
    }
  }

  private void drainAndSend() {
    BufferedTelemetry telemetry = eventConverter.convert(eventBuffer);
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

  RecordingFile openRecordingFile(Path file) {
    try {
      return new RecordingFile(file);
    } catch (IOException e) {
      throw new RuntimeException("Error opening recording file", e);
    }
  }

  void deleteFile(Path dumpFile) {
    try {
      Files.delete(dumpFile);
    } catch (Exception e) {
      // TODO: I think we actually want to log an error here and exit cleanly, rather than
      // throw an exception on the executor thread
      throw new RuntimeException(e);
    }
  }
}
