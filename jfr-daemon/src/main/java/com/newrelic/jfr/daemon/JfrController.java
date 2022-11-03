package com.newrelic.jfr.daemon;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.newrelic.jfr.profiler.ProfileSummarizer;
import com.newrelic.jfr.tometric.ThreadAllocationStatisticsMapper;
import jdk.jfr.EventSettings;
import jdk.jfr.Period;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class JfrController {

  private static final Logger logger = LoggerFactory.getLogger(JfrController.class);

  private final ExecutorService executorService;
  private final JFRUploader uploader;
  private final Duration harvestInterval;

  private volatile boolean shutdown = false;

  public JfrController(JFRUploader uploader, Duration harvestInterval) {
    executorService =
        Executors.newFixedThreadPool(
            5,
            r -> {
              Thread thread = new Thread(r, JfrController.class.getSimpleName());
              thread.setDaemon(true);
              return thread;
            });
    this.uploader = uploader;
    this.harvestInterval = harvestInterval;
    setupStreaming();
  }

  /** Stop the {@link #loop()}. */
  public void shutdown() {
    logger.info("Shutting down JfrController.");
    shutdown = true;
  }

  /**
   * Loop until {@link #shutdown()}, recording and handling JFR data each iteration.
   *
   */
  public void loop() {
    logger.info("Starting JfrController.");
    while (!shutdown) {
      SafeSleep.sleep(harvestInterval);
      executorService.submit(uploader::harvest);
    }
    logger.info("Stopping JfrController. Shutdown detected.");
    executorService.shutdown();
  }

  private void setupStreaming() {
    executorService.submit(this::subscribe);
  }

  private void subscribe() {
    Collection<String> eventNames = uploader.getEventNames();
    try (RecordingStream rs = new RecordingStream()) {
      for (String eventName : eventNames) {
        EventSettings eventSettings = rs.enable(eventName);
        switch (eventName) {
          case ThreadAllocationStatisticsMapper.EVENT_NAME -> eventSettings.with(Period.NAME, "1s");
          case ProfileSummarizer.EVENT_NAME -> eventSettings.with(Period.NAME, "10ms");
          case ProfileSummarizer.NATIVE_EVENT_NAME -> eventSettings.with(Period.NAME, "20ms");
        }
        rs.onEvent(eventName, this::acceptEvent);
      }
      rs.start();
    }
  }

  private void acceptEvent(RecordedEvent event) {
    executorService.submit(() -> uploader.accept(event));
  }
}
