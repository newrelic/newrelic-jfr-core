package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class LastSendStateTrackerTest {

  @Test
  void testNotReady() {
    var testClass = new LastSendStateTracker(Duration.ofDays(500_000), Integer.MAX_VALUE);
    assertFalse(testClass.updateIfReady(531800));
  }

  @Test
  void testReadyDueToSize() {
    var now = Instant.now();
    Supplier<Instant> clock = () -> now;
    var testClass = new LastSendStateTracker(Duration.ofDays(900), 12, clock);
    assertTrue(testClass.updateIfReady(13));
    assertTrue(testClass.updateIfReady(13));
  }

  @Test
  void testReadyDueToTime() {
    var maxSendDuration = Duration.ofMillis(10_000_000);
    var now = Instant.ofEpochMilli(maxSendDuration.toMillis() + 1);
    Supplier<Instant> clock = () -> now;
    var testClass = new LastSendStateTracker(maxSendDuration, 999, clock);
    assertTrue(testClass.updateIfReady(4));
  }

  @Test
  void testUpdateResetsTime() {
    var maxSendDuration = Duration.ofMillis(10_000_000);
    var firstTime = Instant.ofEpochMilli(maxSendDuration.toMillis() + 1);
    var secondTime = Instant.ofEpochMilli(maxSendDuration.toMillis() + 2);
    Supplier<Instant> clock = mock(Supplier.class);
    when(clock.get()).thenReturn(firstTime, secondTime);
    var testClass = new LastSendStateTracker(maxSendDuration, 999, clock);
    assertTrue(testClass.updateIfReady(4));
    assertFalse(testClass.updateIfReady(4));
  }
}
