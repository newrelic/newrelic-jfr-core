package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class SafeSleepTest {

  @Test
  void testEasy() {
    long pre = System.nanoTime();
    SafeSleep.sleep(Duration.ofNanos(120));
    long post = System.nanoTime();
    assertTrue(post > pre);
  }

  @Test
  void testInterrupted() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Thread thread =
        new Thread(
            () -> {
              SafeSleep.sleep(Duration.ofSeconds(15));
              assertTrue(Thread.currentThread().isInterrupted());
              latch.countDown();
            });
    thread.start();
    thread.interrupt();
    assertTrue(latch.await(2, TimeUnit.SECONDS));
  }
}
