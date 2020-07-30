package com.newrelic.jfr.daemon.lifecycle;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
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
public class RemoteEntityGuid {

  private static final Logger logger = LoggerFactory.getLogger(RemoteEntityGuid.class);
  public static final String MBEAN_NAME = "com.newrelic.jfr:type=LinkingMetadata";

  private final MBeanServerConnection mBeanServerConnection;

  public RemoteEntityGuid(MBeanServerConnection mBeanServerConnection) {
    this.mBeanServerConnection = mBeanServerConnection;
  }

  /**
   * Attempts to query a remote LinkingMetadata MBean for the linking metadata so that it can obtain
   * the entity guid. If the remote MBean is not found, this method will return null. When the
   * linking metadata does not yet contain an entity guid, then this code will enter a busy-wait
   * loop, checking every second forever until the entity guid is contained in the linking metadata.
   *
   * @return The entity guid of the remote service or null if the remote service does not expose the
   *     expected MBean.
   */
  public String queryFromJmx() {
    return queryFromJmx(Duration.ofSeconds(1));
  }

  // Exists for testing
  String queryFromJmx(Duration sleepInterval) {
    Callable<Result> callable = this::tryToFetchGuid;
    Predicate<Result> completionCheck =
        result -> result.type == Result.Type.NO_AGENT || result.type == Result.Type.OK;
    var busyWait = new BusyWait<>("Getting entity guid", callable, completionCheck, sleepInterval);
    var result = busyWait.apply();
    if (result.entityGuid != null) {
      logger.info("Entity guid obtained from remote: " + result.entityGuid);
    } else {
      logger.info("No remote agent, no entity guid.");
    }
    return result.entityGuid;
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
      return newMBeanProxy(name);
    } catch (MalformedObjectNameException e) {
      logger.error("Error fetching MBean from remote", e);
      return null;
    }
  }

  // exists for testing
  LinkingMetadataMBean newMBeanProxy(ObjectName name) {
    return JMX.newMBeanProxy(mBeanServerConnection, name, LinkingMetadataMBean.class);
  }

  public interface LinkingMetadataMBean {
    Map<String, String> readLinkingMetadata();
  }

  private static class Result {
    enum Type {
      NO_AGENT,
      NO_GUID,
      OK
    }

    private static final Result NO_AGENT = new Result(Type.NO_AGENT, null);
    private static final Result NO_GUID = new Result(Type.NO_GUID, null);

    private final String entityGuid;
    private final Type type;

    static Result ok(String entityGuid) {
      return new Result(Type.OK, entityGuid);
    }

    private Result(Type type, String entityGuid) {
      this.entityGuid = entityGuid;
      this.type = type;
    }
  }
}
