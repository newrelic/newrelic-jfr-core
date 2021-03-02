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

// jdk.SocketRead {
//        startTime = 15:47:41.648
//        duration = 11.1 ms
//        host = "staging-collector-001.newrelic.com"
//        address = "162.247.241.17"
//        port = 443
//        timeout = 2 m 0 s
//        bytesRead = 5 bytes
//        endOfStream = false
//        eventThread = "New Relic Faster Harvest Service" (javaThreadId = 65)
//        stackTrace = [
//        java.net.SocketInputStream.read(byte[], int, int, int) line: 71
//        java.net.SocketInputStream.read(byte[], int, int) line: 140
//        sun.security.ssl.SSLSocketInputRecord.read(InputStream, byte[], int, int) line: 448
//        sun.security.ssl.SSLSocketInputRecord.bytesInCompletePacket() line: 68
//        sun.security.ssl.SSLSocketImpl.readApplicationRecord(ByteBuffer) line: 1104
//        ...
//        ]
// }

public class NetworkReadSummarizer extends AbstractThreadDispatchingSummarizer {
  public static final String EVENT_NAME = "jdk.SocketRead";

  public NetworkReadSummarizer(ThreadNameNormalizer nameNormalizer) {
    super(nameNormalizer);
  }

  public NetworkReadSummarizer() {
    super();
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
                new PerThreadNetworkReadSummarizer(
                    groupedThreadName, ev.getStartTime().toEpochMilli()));
          }
          perThread.get(groupedThreadName).accept(ev);
        });
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }
}
