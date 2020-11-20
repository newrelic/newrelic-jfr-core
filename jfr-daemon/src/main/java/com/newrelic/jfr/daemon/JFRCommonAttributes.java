/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.COLLECTOR_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.INSTRUMENTATION_PROVIDER;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;

import com.newrelic.telemetry.Attributes;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRCommonAttributes {

  private static final Logger logger = LoggerFactory.getLogger(JFRCommonAttributes.class);
  static final Attributes COMMON_ATTRIBUTES =
      new Attributes()
          .put(INSTRUMENTATION_NAME, "JFR")
          .put(INSTRUMENTATION_PROVIDER, "JFR-Uploader")
          .put(COLLECTOR_NAME, "JFR-Uploader");

  private final DaemonConfig config;
  private final HostInfo hostInfo;

  public JFRCommonAttributes(DaemonConfig config) {
    this(config, HostInfo.DEFAULT);
  }

  JFRCommonAttributes(DaemonConfig config, HostInfo hostInfo) {
    this.config = config;
    this.hostInfo = hostInfo;
  }

  public Attributes build(Optional<String> entityGuid) {
    var hostname = findHostname();
    var attr = COMMON_ATTRIBUTES.put(HOSTNAME, hostname);
    entityGuid.ifPresentOrElse(
        guid -> attr.put("entity.guid", guid),
        () -> {
          attr.put(APP_NAME, config.getMonitoredAppName());
          attr.put(SERVICE_NAME, config.getMonitoredAppName());
        });
    return attr;
  }

  private String findHostname() {
    try {
      return hostInfo.getHost();
    } catch (Throwable e) {
      String loopback = hostInfo.getLoopback();
      logger.error(
          "Unable to get localhost IP, defaulting to loopback address," + loopback + ".", e);
      return loopback;
    }
  }

  interface HostInfo {

    HostInfo DEFAULT = new HostInfo() {};

    default String getLoopback() {
      return InetAddress.getLoopbackAddress().toString();
    }

    default String getHost() throws UnknownHostException {
      return InetAddress.getLocalHost().toString();
    }
  }
}
