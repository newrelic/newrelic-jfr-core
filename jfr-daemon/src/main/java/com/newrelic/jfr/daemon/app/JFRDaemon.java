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
import java.util.Optional;
import javax.management.MBeanServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JFRDaemon {
  private static final Logger logger = LoggerFactory.getLogger(JFRDaemon.class);

  public static void main(String[] args) {
    DaemonConfig config = SetupUtils.buildConfig();
    MBeanConnectionFactory connectionFactory =
        new MBeanConnectionFactory(config.getJmxHost(), config.getJmxPort());
    Attributes commonAttrs = SetupUtils.buildCommonAttributes(config);
    JFRUploader uploader = SetupUtils.buildUploader(config);
    JmxJfrRecorderFactory recorderFactory = new JmxJfrRecorderFactory(config, connectionFactory);
    JfrController controller =
        new JfrController(recorderFactory, uploader, config.getHarvestInterval());

    try {
      // Await initial connection to remote MBean Server.
      MBeanServerConnection connection = connectionFactory.awaitConnection(waitForeverBackoff());

      final RemoteEntityBridge remoteEntityBridge = new RemoteEntityBridge();

      // Asynchronously fetch the remote entity id, and upon completion, mark the uploader as
      // readyToSend. This allows the JFR data to start being recorded while awaiting the
      // entity id to become available.
      remoteEntityBridge
          .fetchRemoteEntityIdAsync(connection)
          .thenAccept(
              optGuid -> {
                optGuid.ifPresent(s -> commonAttrs.put(ENTITY_GUID, s));
                // Asynchronously fetch the remote entity name and override the local config.
                // The entity name should always be available once the entity guid is.
                final Optional<String> remoteAppNameOptional =
                    remoteEntityBridge.getRemoteEntityName(connection);
                if (remoteAppNameOptional.isPresent()) {
                  final String remoteAppName = remoteAppNameOptional.get();
                  commonAttrs.put(SERVICE_NAME, remoteAppName);
                  commonAttrs.put(APP_NAME, remoteAppName);
                }
                uploader.readyToSend(
                    new EventConverter(commonAttrs, config.getThreadNamePattern()));
              });

      controller.loop();
    } catch (Throwable e) {
      logger.error("JFR Daemon is crashing!", e);
      controller.shutdown();
      throw new RuntimeException(e);
    }
  }
}
