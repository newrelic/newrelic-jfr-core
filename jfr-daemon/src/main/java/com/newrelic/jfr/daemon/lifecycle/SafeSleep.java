package com.newrelic.jfr.daemon.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SafeSleep {

    private static final Logger logger = LoggerFactory.getLogger(SafeSleep.class);

    public static void nanos(long nanos) {
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
        } catch (InterruptedException e) {
            logger.warn("Interrupted while sleeping", e);
            Thread.currentThread().interrupt();
        }
    }
}