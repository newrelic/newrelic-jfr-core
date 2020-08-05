package com.newrelic.jfr.daemon;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * This class holds the state around when the lsat time data was sent, and what the max payload size
 * is (in telemetry data points). You can call the isReady() method to check and see if a batch
 * should now be sent, and after sending the last sent time should be updated with updateSendTime().
 */
class LastSendStateTracker {

  private final Duration maxSendDuration;
  private final int maxSizeBeforeSend;
  private final Supplier<Instant> clock;
  private Instant lastTime = Instant.EPOCH;

  public LastSendStateTracker(Duration maxSendDuration, int maxSizeBeforeSend) {
    this(maxSendDuration, maxSizeBeforeSend, Instant::now);
  }

  public LastSendStateTracker(
      Duration maxSendDuration, int maxSizeBeforeSend, Supplier<Instant> clock) {
    this.maxSendDuration = maxSendDuration;
    this.maxSizeBeforeSend = maxSizeBeforeSend;
    this.clock = clock;
  }

  public void updateSendTime() {
    this.lastTime = clock.get();
  }

  public boolean isReady(int totalSize) {
    return bufferIsBigEnough(totalSize) || enoughTimeHasPassed();
  }

  private boolean bufferIsBigEnough(int totalSize) {
    return totalSize > maxSizeBeforeSend;
  }

  private boolean enoughTimeHasPassed() {
    return clock.get().isAfter(lastTime.plus(maxSendDuration));
  }
}
