/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.ThreadNameNormalizer;

/**
 * This class handles all TLAB allocation JFR events, and delegates them to the actual aggregators,
 * which operate on a per-thread basis
 */
public final class ObjectAllocationInNewTLABSummarizer extends AbstractThreadDispatchingSummarizer {

  public static final String EVENT_NAME = "jdk.ObjectAllocationInNewTLAB";

  public ObjectAllocationInNewTLABSummarizer(ThreadNameNormalizer nameNormalizer) {
    super(nameNormalizer);
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public EventToSummary createPerThreadSummarizer(String threadName, long startTimeMs) {
    return new PerThreadObjectAllocationInNewTLABSummarizer(threadName, startTimeMs);
  }
}
