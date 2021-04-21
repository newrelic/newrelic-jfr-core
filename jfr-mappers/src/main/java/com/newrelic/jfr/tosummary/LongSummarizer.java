/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import static com.newrelic.jfr.RecordedObjectValidators.*;

import jdk.jfr.consumer.RecordedEvent;

public class LongSummarizer {
  private static final String SIMPLE_CLASS_NAME = LongSummarizer.class.getSimpleName();

  private final String fieldName;
  private int count = 0;
  private long sum = 0L;
  private long min = Long.MAX_VALUE;
  private long max = Long.MIN_VALUE;

  public LongSummarizer(String fieldName) {
    this.fieldName = fieldName;
  }

  public void accept(RecordedEvent ev) {
    count++;
    long currentValue = 0;
    if (hasField(ev, fieldName, SIMPLE_CLASS_NAME)) {
      currentValue = ev.getLong(fieldName);
    }
    sum = sum + currentValue;

    if (currentValue > max) {
      max = currentValue;
    }
    if (currentValue < min) {
      min = currentValue;
    }
  }

  public void reset() {
    count = 0;
    sum = 0L;
    min = Long.MAX_VALUE;
    max = 0L;
  }

  public int getCount() {
    return count;
  }

  public long getSum() {
    return sum;
  }

  public long getMin() {
    return min;
  }

  public long getMax() {
    return max;
  }
}
