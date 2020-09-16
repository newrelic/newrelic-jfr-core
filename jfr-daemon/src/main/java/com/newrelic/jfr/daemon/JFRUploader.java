/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.*;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JFRUploader {
  private static final Logger logger = LoggerFactory.getLogger(JFRUploader.class);

  public static final Function<Path, RecordingFile> OPEN_RECORDING_FILE =
      file -> {
        try {
          return new RecordingFile(file);
        } catch (IOException e) {
          throw new RuntimeException("Error opening recording file", e);
        }
      };

  private final TelemetryClient telemetryClient;
  private final RecordedEventBuffer recordedEventBuffer;
  private final EventConverter eventConverter;
  private final Function<Path, RecordingFile> recordingFileOpener;
  private final Consumer<Path> fileDeleter;

  public JFRUploader(
      TelemetryClient telemetryClient,
      RecordedEventBuffer recordedEventBuffer,
      EventConverter eventConverter) {
    this(
        telemetryClient,
        recordedEventBuffer,
        eventConverter,
        OPEN_RECORDING_FILE,
        JFRUploader::deleteFile);
  }

  public JFRUploader(
      TelemetryClient telemetryClient,
      RecordedEventBuffer recordedEventBuffer,
      EventConverter eventConverter,
      Function<Path, RecordingFile> recordingFileOpener,
      Consumer<Path> fileDeleter) {
    this.telemetryClient = telemetryClient;
    this.recordedEventBuffer = recordedEventBuffer;
    this.eventConverter = eventConverter;
    this.recordingFileOpener = recordingFileOpener;
    this.fileDeleter = fileDeleter;
  }

  void handleFile(final Path dumpFile) {
    try {
      bufferFileData(dumpFile);
      maybeDrainAndSend();
    } catch (Exception e) {
      logger.error("Error handling raw dump file", e);
    } finally {
      fileDeleter.accept(dumpFile);
    }
  }

  private void bufferFileData(Path dumpFile) {
    try (var recordingFile = recordingFileOpener.apply(dumpFile)) {
      recordedEventBuffer.bufferEvents(dumpFile, recordingFile);
    } catch (Throwable t) {
      logger.error("Error processing file " + dumpFile, t);
    }
  }

  private void maybeDrainAndSend() {
    BufferedTelemetry telemetry = eventConverter.convert(recordedEventBuffer);
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

  private static void deleteFile(Path dumpFile) {
    try {
      Files.delete(dumpFile);
    } catch (Exception e) {
      // TODO: I think we actually want to log an error here and exit cleanly, rather than
      // throw an exception on the executor thread
      throw new RuntimeException(e);
    }
  }
}
