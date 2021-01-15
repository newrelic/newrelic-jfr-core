package com.newrelic.jfr.agent;

import com.newrelic.jfr.daemon.DaemonConfig;
import com.newrelic.jfr.daemon.JFRUploader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.management.*;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AgentController {
  private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

  private final DaemonConfig config;
  private final JFRUploader uploader;
  private Recording recording;

  private volatile boolean shutdown = false;

  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          2,
          r -> {
            Thread result = new Thread(r, "NRJFRAgent");
            result.setDaemon(true);
            return result;
          });

  AgentController(JFRUploader uploader, DaemonConfig config) {
    this.uploader = uploader;
    this.config = config;
  }

  void cleanup() {
    var shouldBeEmpty = executorService.shutdownNow();
    if (shouldBeEmpty.size() > 0) {
      logger.error(
          "Non-empty list of runnable tasks seen, this should not happen: " + shouldBeEmpty);
    }
  }

  void loop(final Duration harvestInterval) throws IOException {
    while (!shutdown) {
      try {
        TimeUnit.MILLISECONDS.sleep(harvestInterval.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // Ignore the premature return and trigger the next dump at once
      }

      final var pathToFile = cloneJfrRecording();
      executorService.submit(() -> uploader.handleFile(pathToFile));
    }
    executorService.shutdown();
  }

  void startRecording() {
    final Configuration jfrConfig;
    try {
      jfrConfig = Configuration.getConfiguration("profile");
    } catch (IOException | ParseException e) {
      // This should never happen
      throw new RuntimeException(e);
    }
    final var recording = new Recording(jfrConfig);
    recording.setMaxAge(config.getHarvestInterval().plus(10, ChronoUnit.SECONDS));
    recording.setToDisk(true);
    recording.setName("New Relic JFR Agent Recording");

    recording.start();
    this.recording = recording;
  }

  Path cloneJfrRecording() throws IOException {
    final var output = Files.createTempFile("local-recording", ".jfr");
    recording.copy(false);
    recording.dump(output);
    return output;
  }
}
