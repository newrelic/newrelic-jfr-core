package com.newrelic.jfr.daemon.lifecycle;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SafeSleepTest {

  @Test
  void testEasy() {
    long pre = System.nanoTime();
    SafeSleep.nanos(120);
    long post = System.nanoTime();
    assertNotEquals(pre, post); // admittedly a silly test
  }

  @Test
  void testInterrupted() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Thread thread =
        new Thread(
            () -> {
              SafeSleep.nanos(TimeUnit.SECONDS.toNanos(15));
              latch.countDown();
            });
    thread.start();
    thread.interrupt();
    latch.await(2, TimeUnit.SECONDS);
    assertTrue(thread.isInterrupted());
  }
}
