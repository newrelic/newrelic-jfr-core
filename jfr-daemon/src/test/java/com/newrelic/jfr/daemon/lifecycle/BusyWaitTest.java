package com.newrelic.jfr.daemon.lifecycle;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BusyWaitTest {

  @Test
  void testSimple() {
    var busyWait = new BusyWait<>("test", () -> "ok");
    assertEquals("ok", busyWait.apply());
  }

  @Test
  void testMultipleInvocations() {
    var invocations = new AtomicInteger(0);
    Callable<String> callable =
        () -> {
          if (invocations.incrementAndGet() < 5) {
            throw new RuntimeException("Whoopsie!");
          }
          return "five";
        };
    var busyWait =
        new BusyWait<>("test", callable, x -> true, Duration.ofMillis(10), Duration.ofSeconds(10));
    assertEquals("five", busyWait.apply());
    assertEquals(5, invocations.get());
  }

  @Test
  void testWithPredicate() {
    var invocations = new AtomicInteger(0);
    Callable<String> callable =
        () -> {
          return "" + invocations.incrementAndGet();
        };
    var busyWait =
        new BusyWait<>(
            "test", callable, x -> x.length() > 2, Duration.ofMillis(1), Duration.ofSeconds(10));
    assertEquals("100", busyWait.apply());
    assertEquals(100, invocations.get());
  }
}
