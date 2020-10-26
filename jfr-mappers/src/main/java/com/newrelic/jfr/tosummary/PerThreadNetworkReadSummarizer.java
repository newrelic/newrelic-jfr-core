/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

public class PerThreadNetworkReadSummarizer implements EventToSummary {
  private final String threadName;
  private final LongSummarizer bytesSummary;
  private final DurationSummarizer duration;

  public PerThreadNetworkReadSummarizer(String threadName, long startTimeMs) {
    this(threadName, new LongSummarizer("bytesRead"), new DurationSummarizer(startTimeMs));
  }

  public PerThreadNetworkReadSummarizer(
      String threadName, LongSummarizer longSummarizer, DurationSummarizer duration) {
    this.threadName = threadName;
    this.bytesSummary = longSummarizer;
    this.duration = duration;
  }

  @Override
  public String getEventName() {
    return NetworkReadSummarizer.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    bytesSummary.accept(ev);
    duration.accept(ev);
  }

  @Override
  public Stream<Summary> summarize() {
    var attr = new Attributes().put("thread.name", threadName);
    var outRead =
        new Summary(
            "jfr.SocketRead.bytesRead",
            bytesSummary.getCount(),
            bytesSummary.getSum(),
            bytesSummary.getMin(),
            bytesSummary.getMax(),
            duration.getStartTimeMs(),
            duration.getEndTimeMs(),
            attr);
    var outDuration =
        new Summary(
            "jfr.SocketRead.duration",
            bytesSummary.getCount(),
            duration.getDurationMillis(),
            duration.getMinDurationMillis(),
            duration.getMaxDurationMillis(),
            duration.getStartTimeMs(),
            outRead.getEndTimeMs(),
            attr);
    return Stream.of(outRead, outDuration);
  }

  public void reset() {
    bytesSummary.reset();
    duration.reset();
  }
}
