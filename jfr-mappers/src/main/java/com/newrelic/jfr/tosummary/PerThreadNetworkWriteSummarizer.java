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

public class PerThreadNetworkWriteSummarizer implements EventToSummary {
  private final String threadName;
  private final LongSummarizer bytesSummary;
  private final SimpleDurationSummarizer duration;

  public PerThreadNetworkWriteSummarizer(String threadName, long startTimeMs) {
    this(threadName, new LongSummarizer("bytesWritten"), new SimpleDurationSummarizer(startTimeMs));
  }

  public PerThreadNetworkWriteSummarizer(
      String threadName, LongSummarizer longSummarizer, SimpleDurationSummarizer duration) {
    this.threadName = threadName;
    this.bytesSummary = longSummarizer;
    this.duration = duration;
  }

  @Override
  public String getEventName() {
    return NetworkWriteSummarizer.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    bytesSummary.accept(ev);
    duration.accept(ev);
  }

  @Override
  public Stream<Summary> summarize() {
    Attributes attr = new Attributes().put("thread.name", threadName);
    Summary outWritten =
        new Summary(
            "jfr.SocketWrite.bytesWritten",
            bytesSummary.getCount(),
            bytesSummary.getSum(),
            bytesSummary.getMin(),
            bytesSummary.getMax(),
            duration.getStartTimeMs(),
            duration.getEndTimeMs(),
            attr);
    Summary outDuration =
        new Summary(
            "jfr.SocketWrite.duration",
            bytesSummary.getCount(),
            duration.getDurationMillis(),
            duration.getMinDurationMillis(),
            duration.getMaxDurationMillis(),
            duration.getStartTimeMs(),
            duration.getEndTimeMs(),
            attr);

    return Stream.of(outWritten, outDuration);
  }

  public void reset() {
    bytesSummary.reset();
    duration.reset();
  }
}
