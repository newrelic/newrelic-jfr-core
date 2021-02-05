/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon.app;

import static com.newrelic.jfr.daemon.app.MBeanConnectionFactory.waitForeverBackoff;

import com.newrelic.jfr.daemon.AttributeNames;
import com.newrelic.jfr.daemon.SafeSleep;
import com.newrelic.telemetry.Backoff;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

  public RemoteEntityBridge() {}

  /**
   * Asynchronously fetch the remote entity id using the {@code connection}. If no remote agent is
   * detected, returns empty. If a remote agent is available, it periodically check if the {@code
   * LinkingMetadata} MBean is available. When it becomes available, it will return the {@link
   * AttributeNames#ENTITY_GUID} property.
   *
   * @param connection the connection to the remote server
   * @return a completable future resolving the remote entity id
   */
  public CompletableFuture<Optional<String>> fetchRemoteEntityIdAsync(
      MBeanServerConnection connection) {
    return CompletableFuture.supplyAsync(
        () -> awaitRemoteEntityId(connection, waitForeverBackoff()));
  }

  Optional<String> awaitRemoteEntityId(MBeanServerConnection connection, Backoff backoff) {
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
    return Optional.ofNullable(metadata.get(AttributeNames.ENTITY_GUID));
  }

  // NOTE: this must be public or NotCompliantMBeanException will be thrown
  public interface LinkingMetadataMBean {
    Map<String, String> readLinkingMetadata();
  }
}
