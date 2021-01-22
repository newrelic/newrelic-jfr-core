/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon.app;

import com.newrelic.jfr.daemon.JfrController;
import com.newrelic.jfr.daemon.SetupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRDaemon {
  private static final Logger logger = LoggerFactory.getLogger(JFRDaemon.class);

  public static void main(String[] args) {
    try {
      var config = SetupUtils.buildConfig();
      var connectionFactory = new MBeanConnectionFactory(config.getJmxHost(), config.getJmxPort());
      var commonAttrs = SetupUtils.buildCommonAttributes();
      new RemoteEntityBridge(connectionFactory)
          .connectAndAppendRemoteAttributes(commonAttrs, config.getMonitoredAppName());
      var uploader = SetupUtils.buildUploader(config, commonAttrs);
      var recorderFactory = new JmxJfrRecorderFactory(config, connectionFactory);

      var controller = new JfrController(recorderFactory, uploader, config.getHarvestInterval());
      controller.loop();
    } catch (Throwable e) {
      logger.error("JFR Daemon is crashing!", e);
      throw new RuntimeException(e);
    }
  }
}
