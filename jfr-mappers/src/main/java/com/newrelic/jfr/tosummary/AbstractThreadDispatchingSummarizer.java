/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.BasicThreadInfo;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.telemetry.metrics.Summary;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public abstract class AbstractThreadDispatchingSummarizer implements EventToSummary {
  protected final Map<String, EventToSummary> perThread = new HashMap<>();

  protected final ThreadNameNormalizer nameNormalizer;

  public AbstractThreadDispatchingSummarizer(ThreadNameNormalizer nameNormalizer) {
    this.nameNormalizer = nameNormalizer;
  }

  public AbstractThreadDispatchingSummarizer() {
    nameNormalizer = null;
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

  public abstract void accept(RecordedEvent ev);

  protected String groupedName(RecordedEvent ev, String threadName) {
    if (nameNormalizer == null) {
      return threadName;
    } else {
      // This is safe, as this method is only called after a successful detection in Workarounds
      RecordedThread rt = ev.getValue("eventThread");
      return nameNormalizer.getNormalizedThreadName(new BasicThreadInfo(rt));
    }
  }
}
