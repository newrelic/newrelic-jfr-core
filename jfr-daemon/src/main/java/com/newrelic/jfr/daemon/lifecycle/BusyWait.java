package com.newrelic.jfr.daemon.lifecycle;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a busy wait loop that tries forever until a result is obtained. This class has a
 * configurable sleep and logging interval. Exceptions when invoking the delegate are considered
 * failures and will continue retrying.
 *
 * @param <T> The final accepted result from the callable.
 */
public class BusyWait<T> {
  private static final Logger logger = LoggerFactory.getLogger(BusyWait.class);
  private final String name;
  private final Callable<T> callable;
  private final Duration interval;
  private final Duration logInterval;
  private final Predicate<T> completionCheck;
  private Instant lastLogTime = Instant.EPOCH;

  public BusyWait(String name, Callable<T> callable) {
    this(name, callable, x -> true);
  }

  public BusyWait(String name, Callable<T> callable, Predicate<T> completionCheck) {
    this(name, callable, completionCheck, Duration.ofSeconds(1), Duration.ofSeconds(10));
  }

  public BusyWait(
      String name,
      Callable<T> callable,
      Predicate<T> completionCheck,
      Duration sleepInterval,
      Duration logInterval) {
    this.name = name;
    this.callable = callable;
    this.completionCheck = completionCheck;
    this.interval = sleepInterval;
    this.logInterval = logInterval;
  }

  public T apply() {
    while (true) {
      try {
        T result = callable.call();
        if (completionCheck.test(result)) {
          logger.debug("Busy wait complete: " + name);
          return result;
        }
      } catch (Exception e) {
        logger.debug("Error while busy waiting for " + name, e);
      }
      if (Instant.now().isAfter(lastLogTime.plus(logInterval.toNanos(), ChronoUnit.NANOS))) {
        logger.info("Busy waiting for " + name + " continues...");
        lastLogTime = Instant.now();
      }
      SafeSleep.nanos(interval.toNanos());
    }
  }
}
