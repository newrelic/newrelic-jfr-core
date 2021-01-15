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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
  private final AtomicReference<EventConverter> eventConverter;
  private final Function<Path, RecordingFile> recordingFileOpener;
  private final Consumer<Path> fileDeleter;
  private final AtomicBoolean readinessCheck;

  private JFRUploader(Builder builder) {
    this.telemetryClient = builder.telemetryClient;
    this.recordedEventBuffer = builder.recordedEventBuffer;
    this.eventConverter = builder.eventConverter;
    this.recordingFileOpener = builder.recordingFileOpener;
    this.fileDeleter = builder.fileDeleter;
    this.readinessCheck = builder.readinessCheck;
  }

  public void handleFile(final Path dumpFile) {
    try {
      bufferFileData(dumpFile);
      maybeDrainAndSend();
    } catch (Exception e) {
      logger.error("Error handling raw dump file", e);
    } finally {
      fileDeleter.accept(dumpFile);
      fileDeleter.accept(dumpFile.getParent());
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
    if (!readinessCheck.get()) {
      logger.warn("Drain attempt skipped -- readiness check not yet ready.");
      return;
    }
    BufferedTelemetry telemetry = eventConverter.get().convert(recordedEventBuffer);
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private TelemetryClient telemetryClient;
    private RecordedEventBuffer recordedEventBuffer;
    private AtomicReference<EventConverter> eventConverter;
    private Function<Path, RecordingFile> recordingFileOpener = OPEN_RECORDING_FILE;
    private Consumer<Path> fileDeleter = JFRUploader::deleteFile;
    private AtomicBoolean readinessCheck;

    public Builder telemetryClient(TelemetryClient telemetryClient) {
      this.telemetryClient = telemetryClient;
      return this;
    }

    public Builder recordedEventBuffer(RecordedEventBuffer recordedEventBuffer) {
      this.recordedEventBuffer = recordedEventBuffer;
      return this;
    }

    public Builder eventConverter(AtomicReference<EventConverter> converter) {
      this.eventConverter = converter;
      return this;
    }

    public Builder recordingFileOpener(Function<Path, RecordingFile> opener) {
      this.recordingFileOpener = opener;
      return this;
    }

    public Builder fileDeleter(Consumer<Path> fileDeleter) {
      this.fileDeleter = fileDeleter;
      return this;
    }

    public Builder readinessCheck(AtomicBoolean readinessCheck) {
      this.readinessCheck = readinessCheck;
      return this;
    }

    public JFRUploader build() {
      return new JFRUploader(this);
    }
  }
}
