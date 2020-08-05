package com.newrelic.jfr.daemon.lifecycle;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows the user to sleep without having to handle InterruptedException. InterruptedExceptions are
 * hidden, but the interrupted state is preserved so that the caller may check it if they need to.
 */
public class SafeSleep {

  private static final Logger logger = LoggerFactory.getLogger(SafeSleep.class);

  public static void nanos(long nanos) {
    try {
      TimeUnit.NANOSECONDS.sleep(nanos);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted while sleeping", e);
    }
  }
}
