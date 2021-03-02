/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr;

import java.lang.management.ThreadInfo;
import jdk.jfr.consumer.RecordedThread;

public class BasicThreadInfo {
  private final long id;
  private final String name;

  public BasicThreadInfo(Thread thread) {
    this(thread.getId(), thread.getName());
  }

  public BasicThreadInfo(RecordedThread thread) {
    this(thread.getJavaThreadId(), thread.getJavaName());
  }

  public BasicThreadInfo(ThreadInfo thread) {
    this(thread.getThreadId(), thread.getThreadName());
  }

  public BasicThreadInfo(long id, String name) {
    super();
    this.id = id;
    this.name = name;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
