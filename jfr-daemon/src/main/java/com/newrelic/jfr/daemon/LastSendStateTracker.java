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

  /**
   * Updates this state tracker's internal "last sent" time in the event that enough time has passed
   * or if the current total batch size is greater than our desired threshold.
   *
   * @param totalBatchSizeInTelemetryPoints - the number of buffered telemetry points
   * @return - true if its time to send and the time was updated, false if not.
   */
  public boolean updateIfReady(int totalBatchSizeInTelemetryPoints) {
    boolean result = bufferIsBigEnough(totalBatchSizeInTelemetryPoints) || enoughTimeHasPassed();
    if (result) {
      this.lastTime = clock.get();
    }
    return result;
  }

  private boolean bufferIsBigEnough(int totalSize) {
    return totalSize > maxSizeBeforeSend;
  }

  private boolean enoughTimeHasPassed() {
    return clock.get().isAfter(lastTime.plus(maxSendDuration));
  }
}
