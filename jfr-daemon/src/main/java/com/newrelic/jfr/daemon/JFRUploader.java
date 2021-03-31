/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.MetricBatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JFRUploader {
  private static final Logger logger = LoggerFactory.getLogger(JFRUploader.class);

  private final TelemetrySender telemetrySender;
  private final RecordedEventBuffer eventBuffer;
  private volatile EventConverter eventConverter;

  public JFRUploader(TelemetrySender telemetrySender, RecordedEventBuffer eventBuffer) {
    this.telemetrySender = telemetrySender;
    this.eventBuffer = eventBuffer;
  }

  /**
   * Handle the JFR {@code dumpFile}. Buffer new events, then convert them and them to New Relic via
   * {@link #telemetrySender}. Finally, delete the file and its parent directory.
   *
   * @param dumpFile the JFR file
   */
  public void handleFile(final Path dumpFile) {
    try {
      bufferFileData(dumpFile);
      maybeDrainAndSend();
    } catch (Exception e) {
      logger.error("Error handling raw dump file", e);
    } finally {
      deleteFile(dumpFile);
      deleteFile(dumpFile.getParent());
    }
  }

  /**
   * Mark the uploader as ready to send events. Until this is called, calls to {@link
   * #handleFile(Path)} will result in JFR events being buffered, but not converted or sent.
   *
   * @param eventConverter the event convert
   */
  public void readyToSend(EventConverter eventConverter) {
    logger.info("JFR Uploader is ready to send events.");
    this.eventConverter = eventConverter;
  }

  private void bufferFileData(Path dumpFile) {
    try (RecordingFile recordingFile = openRecordingFile(dumpFile)) {
      eventBuffer.bufferEvents(dumpFile, recordingFile);
    } catch (Throwable t) {
      logger.error("Error processing file " + dumpFile, t);
    }
  }

  private void maybeDrainAndSend() {
    if (eventConverter == null) {
      logger.warn("Drain attempt skipped because JFRUploader is not yet rady to send.");
      return;
    }
    BufferedTelemetry telemetry = eventConverter.convert(eventBuffer);
    sendMetrics(telemetry);
    sendEvents(telemetry);
  }

  private void sendMetrics(BufferedTelemetry bufferedMetrics) {
    MetricBatch metricBatch = bufferedMetrics.createMetricBatch();
    if (!metricBatch.isEmpty()) {
      logger.info(String.format("Sending metric batch of size %s", metricBatch.size()));
      telemetrySender.sendBatch(metricBatch);
    }
  }

  private void sendEvents(BufferedTelemetry bufferedMetrics) {
    EventBatch eventBatch = bufferedMetrics.createEventBatch();
    if (!eventBatch.isEmpty()) {
      logger.info(String.format("Sending events batch of size %s", eventBatch.size()));
      telemetrySender.sendBatch(eventBatch);
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

  public Instant fileStart() {
    return eventBuffer.start();
  }

  public Instant fileEnd() {
    return eventBuffer.end();
  }
}
