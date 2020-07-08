package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.*;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.TelemetryClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
  static final Attributes COMMON_ATTRIBUTES =
      new Attributes()
          .put(INSTRUMENTATION_NAME, "JFR")
          .put(INSTRUMENTATION_PROVIDER, "JFR-Uploader")
          .put(COLLECTOR_NAME, "JFR-Uploader");

  private final TelemetryClient telemetryClient;
  private final FileToBufferedTelemetry fileToBufferedTelemetry;
  private final Supplier<Instant> nowProvider;
  private final Function<Path, RecordingFile> recordingFileOpener;
  private final Consumer<Path> fileDeleter;

  private Instant lastSeen = null;

  public JFRUploader(
      TelemetryClient telemetryClient, FileToBufferedTelemetry fileToBufferedTelemetry) {
    this(
        telemetryClient,
        fileToBufferedTelemetry,
        Instant::now,
        OPEN_RECORDING_FILE,
        JFRUploader::deleteFile);
  }

  public JFRUploader(
      TelemetryClient telemetryClient,
      FileToBufferedTelemetry fileToBufferedTelemetry,
      Supplier<Instant> nowProvider,
      Function<Path, RecordingFile> recordingFileOpener,
      Consumer<Path> fileDeleter) {
    this.telemetryClient = telemetryClient;
    this.fileToBufferedTelemetry = fileToBufferedTelemetry;
    this.nowProvider = nowProvider;
    this.recordingFileOpener = recordingFileOpener;
    this.fileDeleter = fileDeleter;
  }

  void handleFile(final Path dumpFile) {
    // At startup should we read all the events present? This could be multiple hours of recordings
    if (lastSeen == null) {
      lastSeen = nowProvider.get();
    }

    try (var recordingFile = recordingFileOpener.apply(dumpFile)) {
      logger.debug("Looking in " + dumpFile + " for events after: " + lastSeen);

      var result = fileToBufferedTelemetry.convert(recordingFile, lastSeen, dumpFile.toString());
      lastSeen = result.getLastSeen();
      var bufferedMetrics = result.getBufferedTelemetry();

      sendMetrics(bufferedMetrics);
      sendEvents(bufferedMetrics);
    } catch (Throwable t) {
      logger.error("Error processing file " + dumpFile, t);
    } finally {
      fileDeleter.accept(dumpFile);
    }
  }

  private void sendMetrics(BufferedTelemetry bufferedMetrics) {
    var metricBatch = bufferedMetrics.createMetricBatch();
    if (!metricBatch.isEmpty()) {
      logger.debug(String.format("Sending metric batch of size %s", metricBatch.size()));
      telemetryClient.sendBatch(metricBatch);
    }
  }

  private void sendEvents(BufferedTelemetry bufferedMetrics) {
    var eventBatch = bufferedMetrics.createEventBatch();
    if (!eventBatch.isEmpty()) {
      logger.debug(String.format("Sending events batch of size %s", eventBatch.size()));
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
