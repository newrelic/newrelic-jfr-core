package com.newrelic.jfr.daemon;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JFRController is a long running instance that uses a ScheduledExecutorService to periodically
 * do work. This work is in the method doSingleRecording, which initiates a recording via the
 * JFRJMXRecorder, and then passes the path to the recorded file to the DumpFileProcessor.
 *
 * <p>In addition to the periodic job setup, this class also handles exceptions that can occur when
 * recording or processing (on the read side) and is capable of initiating a reconnect.
 */
public class JFRController {
  private static final Logger logger = LoggerFactory.getLogger(JFRController.class);

  private final DumpFileProcessor dumpFileProcessor;
  private final DaemonConfig config;
  // Non-final to allow for reconnect - there's too much crufty JMX state too close to the surface
  private JFRJMXRecorder recorder;
  private final ScheduledExecutorService executorService;

  public JFRController(
      DumpFileProcessor dumpFileProcessor,
      DaemonConfig config,
      ScheduledExecutorService executorService) {
    this.dumpFileProcessor = dumpFileProcessor;
    this.config = config;
    this.executorService = executorService;
  }

  // This needs to be exposed to JMX / k8s
  public void shutdown() {
    executorService.shutdown();
  }

  void setup() {
    try {
      restartRecording();
    } catch (Exception e) {
      e.printStackTrace();
      shutdown();
      throw new RuntimeException(e);
    }
  }

  void runUntilShutdown() throws InterruptedException {
    var harvestInterval = config.getHarvestInterval();
    executorService.scheduleAtFixedRate(
        this::doSingleRecording, 0, harvestInterval.toMillis(), TimeUnit.MILLISECONDS);
    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    logger.info("JFR Controller is shut down (gracefully)");
  }

  private void doSingleRecording() {
    try {
      final var pathToFile = recorder.recordToFile();
      dumpFileProcessor.handleFile(pathToFile);
    } catch (MalformedObjectNameException
        | MBeanException
        | IOException
        | InstanceNotFoundException
        | OpenDataException
        | ReflectionException e) {
      logger.error("JMX streaming failed: ", e);
      tryToRestart();
    }
  }

  private void tryToRestart() {
    try {
      restartRecording();
    } catch (MalformedObjectNameException
        | MBeanException
        | IOException
        | InstanceNotFoundException
        | OpenDataException
        | ReflectionException e) {
      logger.error("Could not restart recording. JFR Daemon shutting down", e);
      shutdown();
    }
  }

  private void restartRecording()
      throws IOException, MalformedObjectNameException, ReflectionException,
          InstanceNotFoundException, MBeanException, OpenDataException {
    recorder = connect();
    recorder.startRecordingWithBackOff();
  }

  // Exists for testing
  JFRJMXRecorder connect() throws IOException {
    return JFRJMXRecorder.connectWithBackOff(config);
  }
}
