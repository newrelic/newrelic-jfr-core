package com.newrelic.jfr.daemon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenDataException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class JFRController {
  private static final Logger logger = LoggerFactory.getLogger(JFRController.class);

  private final JFRUploader uploader;
  private final DaemonConfig config;
  // Non-final to allow for reconnect - there's too much crufty JMX state too close to the surface
  private JFRJMXRecorder recorder;

  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          2,
          r -> {
            Thread result = new Thread(r, "JFRController");
            result.setDaemon(true);
            return result;
          });

  private volatile boolean shutdown = false;

  public JFRController(JFRUploader uploader, DaemonConfig config) {
    this.uploader = uploader;
    this.config = config;
  }

  // This needs to be exposed to JMX / k8s
  public void shutdown() {
    shutdown = true;
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

  void loop(Duration harvestInterval) throws IOException {
    while (!shutdown) {
      try {
        TimeUnit.MILLISECONDS.sleep(harvestInterval.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // Ignore the premature return and trigger the next JMX dump at once
      }
      try {
        final var pathToFile = recorder.recordToFile();
        executorService.submit(() -> uploader.handleFile(pathToFile));
      } catch (MalformedObjectNameException
          | MBeanException
          | InstanceNotFoundException
          | OpenDataException
          | ReflectionException e) {
        logger.error("JMX streaming failed: ", e);
        try {
          restartRecording();
        } catch (MalformedObjectNameException
            | MBeanException
            | InstanceNotFoundException
            | OpenDataException
            | ReflectionException jmxException) {
          // Log before fatal exit?
          shutdown();
        }
      }
    }
    executorService.shutdown();
  }

  private void restartRecording() throws IOException, MalformedObjectNameException, ReflectionException, InstanceNotFoundException, MBeanException, OpenDataException {
    recorder = JFRJMXRecorder.connectWithBackOff(config);
    recorder.startRecordingWithBackOff();
  }
}
