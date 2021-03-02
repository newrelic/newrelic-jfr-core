/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.jfr.Workarounds;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

/**
 * This class handles all TLAB allocation JFR events, and delegates them to the actual aggregators,
 * which operate on a per-thread basis
 */
public final class ObjectAllocationInNewTLABSummarizer extends AbstractThreadDispatchingSummarizer {

  public static final String EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";

  public ObjectAllocationInNewTLABSummarizer(ThreadNameNormalizer nameNormalizer) {
    super(nameNormalizer);
  }

  public ObjectAllocationInNewTLABSummarizer() {
    super();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    Optional<String> possibleThreadName = Workarounds.getThreadName(ev);
    possibleThreadName.ifPresent(
        threadName -> {
          final String groupedThreadName = groupedName(ev, threadName);
          if (perThread.get(groupedThreadName) == null) {
            perThread.put(
                groupedThreadName,
                new PerThreadObjectAllocationInNewTLABSummarizer(
                    groupedThreadName, ev.getStartTime().toEpochMilli()));
          }
          perThread.get(groupedThreadName).accept(ev);
        });
  }
}
