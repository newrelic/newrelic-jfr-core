/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon.lifecycle;

import com.newrelic.jfr.daemon.DaemonConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class to assist in robustly getting an MBeanServerConnection to the remote JMX server. */
public class MBeanServerConnector {

  private static final Logger logger = LoggerFactory.getLogger(MBeanServerConnector.class);

  private final DaemonConfig config;

  public MBeanServerConnector(DaemonConfig config) {
    this.config = config;
  }

  /**
   * Performs a busy wait in order to get a connection to the remote mbean server. It will retry
   * every second until a connection can be established.
   *
   * @return a new MBeanServerConnection instance
   */
  public MBeanServerConnection getConnection() {
    return getConnection(Duration.ofSeconds(1));
  }

  // for testing
  MBeanServerConnection getConnection(Duration retryInterval) {
    Callable<MBeanServerConnection> callable =
        () -> {
          //        var map = new HashMap<String, Object>();
          //        var credentials = new String[]{"", ""};
          //        map.put("jmx.remote.credentials", credentials);
          var urlPath =
              "/jndi/rmi://" + config.getJmxHost() + ":" + config.getJmxPort() + "/jmxrmi";
          var url = new JMXServiceURL("rmi", "", 0, urlPath);
          var connector = newJMXConnector(url);
          connector.connect();
          MBeanServerConnection result = connector.getMBeanServerConnection();
          logger.info("Connection to remote MBean server complete.");
          return result;
        };
    var busyWait = new BusyWait<>("MBeanServerConnection", callable, x -> true, retryInterval);
    return busyWait.apply();
  }

  // Exists for testing
  JMXConnector newJMXConnector(JMXServiceURL url) throws IOException {
    return JMXConnectorFactory.newJMXConnector(url, null);
  }
}
