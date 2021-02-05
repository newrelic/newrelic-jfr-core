/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeSleep {

  private static final Logger logger = LoggerFactory.getLogger(SafeSleep.class);

  public static void sleep(Duration duration) {
    try {
      TimeUnit.NANOSECONDS.sleep(duration.toNanos());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted while sleeping", e);
    }
  }
}
