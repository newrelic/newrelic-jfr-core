/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.MetricBatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JFRUploader {
  private static final Logger logger = LoggerFactory.getLogger(JFRUploader.class);

  private final TelemetrySender telemetrySender;
  private volatile EventConverter eventConverter;

  public JFRUploader(TelemetrySender telemetrySender) {
    this.telemetrySender = telemetrySender;
  }

  public void readyToSend(EventConverter eventConverter) {
    logger.info("JFR Uploader is ready to send events.");
    this.eventConverter = eventConverter;

  }

  public void accept(RecordedEvent recordedEvent) {
    if (eventConverter == null) {
      logger.warn("Event record skipped because JFRUploader is not yet ready to send.");
      return;
    }
    eventConverter.convertAndBuffer(recordedEvent);
  }

  public void harvest() {
    if (eventConverter == null) {
      logger.warn("Harvest attempt skipped because JFRUploader is not yet ready to send.");
      return;
    }
    BufferedTelemetry telemetry = eventConverter.harvest();
    sendMetrics(telemetry);
    sendEvents(telemetry);
  }

  private void sendMetrics(BufferedTelemetry bufferedMetrics) {
    MetricBatch metricBatch = bufferedMetrics.createMetricBatch();
    if (!metricBatch.isEmpty()) {
      logger.debug("Sending metric batch of size {}", metricBatch.size());
      telemetrySender.sendBatch(metricBatch);
    }
  }

  private void sendEvents(BufferedTelemetry bufferedMetrics) {
    EventBatch eventBatch = bufferedMetrics.createEventBatch();
    if (!eventBatch.isEmpty()) {
      logger.debug("Sending events batch of size {}", eventBatch.size());
      telemetrySender.sendBatch(eventBatch);
    }
  }

  /* Counts how many times the substring appears in the larger string. */
  public static int countMatches(String text, String str) {
    Matcher matcher = Pattern.compile(str).matcher(text);

    int count = 0;
    while (matcher.find()) {
      count++;
    }

    return count;
  }

  // This should only be called after readyToSend
  public Collection<String> getEventNames() {
    return eventConverter.getEventNames();
  }
}
