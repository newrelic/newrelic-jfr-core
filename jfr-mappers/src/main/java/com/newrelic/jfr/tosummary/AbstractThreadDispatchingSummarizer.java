/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.metrics.Summary;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;

public abstract class AbstractThreadDispatchingSummarizer implements EventToSummary {
  protected final Map<String, EventToSummary> perThread = new HashMap<>();

  protected final ThreadNameNormalizer nameNormalizer;

  public AbstractThreadDispatchingSummarizer(ThreadNameNormalizer nameNormalizer) {
    this.nameNormalizer = nameNormalizer;
  }

  @Override
  public Stream<Summary> summarize() {
    return perThread.values().stream().flatMap(EventToSummary::summarize);
  }

  @Override
  public void reset() {
    perThread.clear();
  }

  public abstract String getEventName();

  public abstract EventToSummary createPerThreadSummarizer(String threadName, long startTimeMs);

  @Override
  public void accept(RecordedEvent ev) {
    final Optional<String> possibleGroupedThreadName = groupedName(ev);
    possibleGroupedThreadName.ifPresent(
        groupedThreadName -> {
          if (perThread.get(groupedThreadName) == null) {
            perThread.put(
                groupedThreadName,
                createPerThreadSummarizer(groupedThreadName, ev.getStartTime().toEpochMilli()));
          }
          perThread.get(groupedThreadName).accept(ev);
        });
  }

  protected Optional<String> groupedName(RecordedEvent ev) {
    Optional<String> possibleThreadName = Workarounds.getThreadName(ev);

    if (possibleThreadName.isPresent()) {
      String normalizedThreadName =
          nameNormalizer.getNormalizedThreadName(possibleThreadName.get());
      return Optional.ofNullable(normalizedThreadName);
    }
    return Optional.empty();
  }
}
