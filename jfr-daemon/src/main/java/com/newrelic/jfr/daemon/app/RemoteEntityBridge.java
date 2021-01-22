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

import com.newrelic.jfr.daemon.AttributeNames;
import com.newrelic.jfr.daemon.SafeSleep;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.Backoff;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attempts to locate an entity guid string from a remote MBean. This guid can be used to link jfr
 * daemon data to an external entity.
 */
public class RemoteEntityBridge {

  private static final Logger logger = LoggerFactory.getLogger(RemoteEntityBridge.class);

  private final MBeanConnectionFactory connectionFactory;

  public RemoteEntityBridge(MBeanConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  /**
   * Connect to the remote MBean Server and append relevant attributes. If the remote server has the
   * New Relic Java Agent running, obtain and set the {@link AttributeNames#ENTITY_GUID}. If the New
   * Relic Java Agent is not running, set the {@link AttributeNames#APP_NAME} and {@link
   * AttributeNames#SERVICE_NAME} to the given {@code appName}.
   *
   * <p>This will attempt to connect to the remote server indefinitely.
   *
   * @param attributes the attributes to append to
   * @param appName the app name
   * @throws Exception if an unknown error occurs connecting to the remote server
   */
  public void connectAndAppendRemoteAttributes(Attributes attributes, String appName)
      throws Exception {
    MBeanServerConnection connection = connectionFactory.awaitConnection(waitForeverBackoff());
    Optional<String> optGuid = awaitRemoteEntityGuid(connection, waitForeverBackoff());
    if (optGuid.isPresent()) {
      attributes.put(ENTITY_GUID, optGuid.get());
    } else {
      attributes.put(APP_NAME, appName);
      attributes.put(SERVICE_NAME, appName);
    }
  }

  private static Backoff waitForeverBackoff() {
    return Backoff.builder()
        .maxBackoff(15, TimeUnit.SECONDS)
        .backoffFactor(1, TimeUnit.SECONDS)
        .maxRetries(Integer.MAX_VALUE)
        .build();
  }

  Optional<String> awaitRemoteEntityGuid(MBeanServerConnection connection, Backoff backoff) {
    while (true) {
      Optional<String> optGuid;
      try {
        optGuid = getRemoteEntityGuid(connection);
      } catch (Exception e) {
        logger.info("Unable to identify remote agent. Not setting entity guid.");
        return Optional.empty();
      }
      if (optGuid.isPresent()) {
        logger.info("Obtained entity guid from remote agent: {}", optGuid.get());
        return optGuid;
      }
      long backoffMillis = backoff.nextWaitMs();
      if (backoffMillis == -1) {
        logger.info(
            "Remote agent identified but unable to obtain guid after backing off. Not setting entity guid.");
        return Optional.empty();
      }
      logger.info(
          "Remote agent identified but entity guid not yet available. Backing off {} millis.",
          backoffMillis);
      SafeSleep.sleep(Duration.ofMillis(backoffMillis));
    }
  }

  Optional<String> getRemoteEntityGuid(MBeanServerConnection connection)
      throws MalformedObjectNameException {
    ObjectName name = new ObjectName("com.newrelic.jfr:type=LinkingMetadata");
    LinkingMetadataMBean linkingMetadataMBean =
        JMX.newMBeanProxy(connection, name, LinkingMetadataMBean.class);
    Map<String, String> metadata = linkingMetadataMBean.readLinkingMetadata();
    return Optional.ofNullable(metadata.get("entity.guid"));
  }

  interface LinkingMetadataMBean {
    Map<String, String> readLinkingMetadata();
  }
}
