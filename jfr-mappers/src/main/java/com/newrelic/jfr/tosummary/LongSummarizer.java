/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import jdk.jfr.consumer.RecordedEvent;

public class LongSummarizer {

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
    long currentValue = ev.getLong(fieldName);
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
