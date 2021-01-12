/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.Workarounds;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

// jdk.SocketWrite {
//        startTime = 20:22:57.161
//        duration = 87.4 ms
//        host = "mysql-staging-agentdb-2"
//        address = "10.31.1.15"
//        port = 3306
//        bytesWritten = 34 bytes
//        eventThread = "ActivityWriteDaemon" (javaThreadId = 252)
//        stackTrace = [
//        java.net.SocketOutputStream.socketWrite(byte[], int, int) line: 68
//        java.net.SocketOutputStream.write(byte[], int, int) line: 150
//        java.io.BufferedOutputStream.flushBuffer() line: 81
//        java.io.BufferedOutputStream.flush() line: 142
//        com.mysql.cj.protocol.a.SimplePacketSender.send(byte[], int, byte) line: 55
//        ...
//        ]
// }

public class NetworkWriteSummarizer extends AbstractThreadDispatchingSummarizer {
  public static final String EVENT_NAME = "jdk.SocketWrite";

  @Override
  public void accept(RecordedEvent ev) {
    Optional<String> possibleThreadName = Workarounds.getThreadName(ev);
    possibleThreadName.ifPresent(
        threadName -> {
          if (perThread.get(threadName) == null) {
            perThread.put(
                threadName,
                new PerThreadNetworkWriteSummarizer(threadName, ev.getStartTime().toEpochMilli()));
          }
          perThread.get(threadName).accept(ev);
        });
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
