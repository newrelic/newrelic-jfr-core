package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.*;

import com.newrelic.telemetry.Attributes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Add javadoc for this class
public final class DumpFileProcessor {
  private static final Logger logger = LoggerFactory.getLogger(DumpFileProcessor.class);

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

  private final FileToRawEventSource fileToRawEventSource;
  private final Function<Path, RecordingFile> recordingFileOpener;
  private final Consumer<Path> fileDeleter;
  private Optional<Instant> lastSeen = Optional.empty();

  public DumpFileProcessor(FileToRawEventSource fileToRawEventSource) {
    this(fileToRawEventSource, OPEN_RECORDING_FILE, DumpFileProcessor::deleteFile);
  }

  public DumpFileProcessor(
      FileToRawEventSource fileToRawEventSource,
      Function<Path, RecordingFile> recordingFileOpener,
      Consumer<Path> fileDeleter) {
    this.fileToRawEventSource = fileToRawEventSource;
    this.recordingFileOpener = recordingFileOpener;
    this.fileDeleter = fileDeleter;
  }

  void handleFile(final Path dumpFile) {
    // At startup should we read all the events present? This could be multiple hours of recordings
    Instant eventsAfter = lastSeen.orElse(Instant.EPOCH);

    try (var recordingFile = recordingFileOpener.apply(dumpFile)) {
      logger.debug("Looking in " + dumpFile + " for events after: " + eventsAfter);
      var newLastSeen =
          fileToRawEventSource.queueRawEvents(recordingFile, eventsAfter, dumpFile.toString());
      lastSeen = Optional.of(newLastSeen);
    } catch (Throwable t) {
      logger.error("Error processing file " + dumpFile, t);
    } finally {
      deleteSafely(dumpFile);
    }
  }

  private void deleteSafely(Path dumpFile) {
    try {
      fileDeleter.accept(dumpFile);
    } catch (Exception e) {
      logger.warn("Error deleting file " + dumpFile, e);
    }
  }

  private static void deleteFile(Path dumpFile) {
    try {
      Files.delete(dumpFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // Exists only for testing
  Optional<Instant> getLastSeen() {
    return lastSeen;
  }
}
