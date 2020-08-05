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

public final class JFRController {
  private static final Logger logger = LoggerFactory.getLogger(JFRController.class);

  private final DumpFileProcessor uploader;
  private final DaemonConfig config;
  // Non-final to allow for reconnect - there's too much crufty JMX state too close to the surface
  private JFRJMXRecorder recorder;
  private final ScheduledExecutorService executorService;

  public JFRController(
      DumpFileProcessor uploader, DaemonConfig config, ScheduledExecutorService executorService) {
    this.uploader = uploader;
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
      uploader.handleFile(pathToFile);
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
    recorder = JFRJMXRecorder.connectWithBackOff(config);
    recorder.startRecordingWithBackOff();
  }
}
