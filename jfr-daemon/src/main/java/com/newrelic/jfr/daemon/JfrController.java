package com.newrelic.jfr.daemon;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the continuous processing of JFR data. {@link #loop()} repeatedly calls {@link
 * JfrRecorder#recordToFile()} and uploads the data via {@link JFRUploader#handleFile(Path)}.
 */
public class JfrController {

  private static final Logger logger = LoggerFactory.getLogger(JfrController.class);

  private final ExecutorService executorService;
  private final JfrRecorderFactory recorderFactory;
  private final JFRUploader uploader;
  private final Duration harvestInterval;

  private volatile boolean shutdown = false;
  private JfrRecorder jfrRecorder;

  public JfrController(
      JfrRecorderFactory recorderFactory, JFRUploader uploader, Duration harvestInterval) {
    executorService =
        Executors.newFixedThreadPool(
            2,
            r -> {
              Thread thread = new Thread(r, JfrController.class.getSimpleName());
              thread.setDaemon(true);
              return thread;
            });
    this.recorderFactory = recorderFactory;
    this.uploader = uploader;
    this.harvestInterval = harvestInterval;
  }

  /** Stop the {@link #loop()}. */
  public void shutdown() {
    logger.info("Shutting down JfrController.");
    shutdown = true;
  }

  /**
   * Loop until {@link #shutdown()}, recording and handling JFR data each iteration.
   *
   * @throws Exception if a fatal error occurs preventing JFR recording / handling from continuing
   */
  public void loop() throws JfrRecorderException {
    logger.info("Starting JfrController.");
    while (!shutdown) {
      SafeSleep.sleep(harvestInterval);

      // Setup jfrRecorder if first iteration
      if (jfrRecorder == null) {
        resetJfrRecorder();
      }

      try {
        Path pathToFile = jfrRecorder.recordToFile();
        executorService.submit(() -> uploader.handleFile(pathToFile));
      } catch (JfrRecorderException e) {
        // If an error occurs, recording to file, attempt to reset the recorder. If
        // resetting fails allow the exception to propagate.
        logger.warn(
            "An error occurred recording JFR to file, resetting recorder: {}", e.getMessage());
        resetJfrRecorder();
      }
    }
    logger.info("Stopping JfrController. Shutdown detected.");
    executorService.shutdown();
  }

  private void resetJfrRecorder() throws JfrRecorderException {
    jfrRecorder = recorderFactory.getRecorder();
  }
}
