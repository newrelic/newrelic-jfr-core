/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon.lifecycle;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
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
public class RemoteEntityGuidCheck {

  private static final Logger logger = LoggerFactory.getLogger(RemoteEntityGuidCheck.class);
  public static final String MBEAN_NAME = "com.newrelic.jfr:type=LinkingMetadata";

  private final Consumer<Optional<String>> onComplete;
  private final ScheduledExecutorService executorService;
  private final Function<ObjectName, LinkingMetadataMBean> mBeanProxyCreator;

  public RemoteEntityGuidCheck(Builder builder) {
    this.onComplete = builder.onComplete;
    this.executorService = builder.executorService;
    this.mBeanProxyCreator = builder.mBeanProxyCreator;
    ;
  }

  /**
   * Attempts to poll a remote LinkingMetadata MBean every second for the linking metadata so that
   * it can obtain the entity guid. If the remote MBean is not found, this method invokes the
   * onComplete callback with an empty Optional. When the MBean is found, it is polled until the
   * entity guid is obtained. Finally, when the entity guid is available the callback is invoked.
   */
  public void start() {
    start(Duration.ofSeconds(1));
  }

  void start(Duration duration) {
    executorService.scheduleAtFixedRate(
        () -> {
          Result result = tryToFetchGuid();
          if (result.type == Result.Type.NO_AGENT || result.type == Result.Type.OK) {
            result.entityGuid.ifPresentOrElse(
                guid -> logger.info("Entity guid obtained from remote: " + guid),
                () -> logger.info("No remote agent, no entity guid."));
            onComplete.accept(result.entityGuid);
            executorService.shutdown();
          }
        },
        0,
        duration.toMillis(),
        MILLISECONDS);
  }

  private Result tryToFetchGuid() {
    var linkingMetadataMBean = buildMBeanProxy();
    if (linkingMetadataMBean == null) {
      return Result.NO_AGENT;
    }
    try {
      var metadata = linkingMetadataMBean.readLinkingMetadata();
      var guid = metadata.get("entity.guid");
      return guid == null ? Result.NO_GUID : Result.ok(guid);
    } catch (Exception e) {
      return Result.NO_AGENT;
    }
  }

  private LinkingMetadataMBean buildMBeanProxy() {
    try {
      ObjectName name = new ObjectName(MBEAN_NAME);
      return mBeanProxyCreator.apply(name);
    } catch (MalformedObjectNameException e) {
      logger.error("Error fetching MBean from remote", e);
      return null;
    }
  }

  // exists for testing
  //  LinkingMetadataMBean newMBeanProxy(ObjectName name) {
  //    return JMX.newMBeanProxy(mBeanServerConnection, name, LinkingMetadataMBean.class);
  //  }

  public interface LinkingMetadataMBean {
    Map<String, String> readLinkingMetadata();
  }

  public static Builder builder() {
    return new Builder();
  }

  private static class Result {
    enum Type {
      NO_AGENT,
      NO_GUID,
      OK
    }

    private static final Result NO_AGENT = new Result(Type.NO_AGENT);
    private static final Result NO_GUID = new Result(Type.NO_GUID);

    private final Optional<String> entityGuid;
    private final Type type;

    static Result ok(String entityGuid) {
      return new Result(Type.OK, Optional.of(entityGuid));
    }

    private Result(Type type) {
      this(type, Optional.empty());
    }

    private Result(Type type, Optional<String> entityGuid) {
      this.entityGuid = entityGuid;
      this.type = type;
    }
  }

  public static class Builder {

    private Function<ObjectName, LinkingMetadataMBean> mBeanProxyCreator;
    private Consumer<Optional<String>> onComplete;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public Builder mbeanServerConnection(MBeanServerConnection conn) {
      this.mBeanProxyCreator = name -> JMX.newMBeanProxy(conn, name, LinkingMetadataMBean.class);
      return this;
    }

    public Builder mBeanProxyCreator(Function<ObjectName, LinkingMetadataMBean> mBeanProxyCreator) {
      this.mBeanProxyCreator = mBeanProxyCreator;
      return this;
    }

    public Builder onComplete(Consumer<Optional<String>> onComplete) {
      this.onComplete = onComplete;
      return this;
    }

    // Exists for testing
    Builder executorService(ScheduledExecutorService service) {
      this.executorService = service;
      return this;
    }

    public RemoteEntityGuidCheck build() {
      return new RemoteEntityGuidCheck(this);
    }
  }
}
