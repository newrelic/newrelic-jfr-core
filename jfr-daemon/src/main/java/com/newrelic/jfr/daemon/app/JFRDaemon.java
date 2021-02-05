/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon.app;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.ENTITY_GUID;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;
import static com.newrelic.jfr.daemon.app.MBeanConnectionFactory.waitForeverBackoff;

import com.newrelic.jfr.daemon.DaemonConfig;
import com.newrelic.jfr.daemon.EventConverter;
import com.newrelic.jfr.daemon.JFRUploader;
import com.newrelic.jfr.daemon.JfrController;
import com.newrelic.jfr.daemon.SetupUtils;
import com.newrelic.telemetry.Attributes;
import javax.management.MBeanServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRDaemon {
  private static final Logger logger = LoggerFactory.getLogger(JFRDaemon.class);

  public static void main(String[] args) {
    try {
      DaemonConfig config = SetupUtils.buildConfig();
      MBeanConnectionFactory connectionFactory =
          new MBeanConnectionFactory(config.getJmxHost(), config.getJmxPort());
      Attributes commonAttrs = SetupUtils.buildCommonAttributes();
      JFRUploader uploader = SetupUtils.buildUploader(config);

      // Await initial connection to remote MBean Server.
      MBeanServerConnection connection = connectionFactory.awaitConnection(waitForeverBackoff());

      // Asynchronously fetch the remote entity id, and upon completion, mark the uploader as
      // readyToSend. This allows the JFR data to start being recorded while awaiting the
      // entity id to become available.
      new RemoteEntityBridge()
          .fetchRemoteEntityIdAsync(connection)
          .thenAccept(
              optGuid -> {
                if (optGuid.isPresent()) {
                  commonAttrs.put(ENTITY_GUID, optGuid.get());
                } else {
                  commonAttrs.put(APP_NAME, config.getMonitoredAppName());
                  commonAttrs.put(SERVICE_NAME, config.getMonitoredAppName());
                }
                uploader.readyToSend(new EventConverter(commonAttrs));
              });

      JmxJfrRecorderFactory recorderFactory = new JmxJfrRecorderFactory(config, connectionFactory);
      JfrController controller =
          new JfrController(recorderFactory, uploader, config.getHarvestInterval());
      controller.loop();
    } catch (Throwable e) {
      logger.error("JFR Daemon is crashing!", e);
      throw new RuntimeException(e);
    }
  }
}
