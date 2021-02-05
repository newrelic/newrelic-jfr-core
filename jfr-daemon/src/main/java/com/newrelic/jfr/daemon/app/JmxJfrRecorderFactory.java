package com.newrelic.jfr.daemon.app;

import com.newrelic.jfr.daemon.DaemonConfig;
import com.newrelic.jfr.daemon.JfrRecorder;
import com.newrelic.jfr.daemon.JfrRecorderException;
import com.newrelic.jfr.daemon.JfrRecorderFactory;
import com.newrelic.jfr.daemon.SafeSleep;
import com.newrelic.telemetry.Backoff;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link JfrRecorderFactory} that produces {@link JmxJfrRecorder}s which
 * record JFR data from a remote MBean server over JMX.
 */
public class JmxJfrRecorderFactory implements JfrRecorderFactory {

  private static final Logger logger = LoggerFactory.getLogger(JmxJfrRecorder.class);

  private final MBeanConnectionFactory connectionFactory;
  private final Duration harvestInterval;
  private final boolean streamFromJmx;

  public JmxJfrRecorderFactory(
      DaemonConfig daemonConfig, MBeanConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
    this.harvestInterval = daemonConfig.getHarvestInterval();
    this.streamFromJmx = daemonConfig.streamFromJmx();
  }

  @Override
  public JfrRecorder getRecorder() throws JfrRecorderException {
    try {
      MBeanServerConnection connection =
          connectionFactory.awaitConnection(Backoff.defaultBackoff());
      long recordingId = startRecordingWithBackoff(connection);
      return new JmxJfrRecorder(connection, streamFromJmx, recordingId);
    } catch (Exception e) {
      throw new JfrRecorderException("Failed to obtain JfrRecorder.", e);
    }
  }

  private long startRecordingWithBackoff(MBeanServerConnection connection) throws Exception {
    Backoff backoff = Backoff.defaultBackoff();
    while (true) {
      try {
        return startRecording(connection);
      } catch (Exception e) {
        long backoffMillis = backoff.nextWaitMs();
        if (backoffMillis == -1) {
          throw new Exception("Failed to start recording after completing backoff.", e);
        } else {
          logger.info("Error starting recording. Backing off {} millis.", backoffMillis);
          SafeSleep.sleep(Duration.ofMillis(backoffMillis));
        }
      }
    }
  }

  private long startRecording(MBeanServerConnection connection) throws Exception {
    logger.debug("In startRecording()");

    ObjectName objectName = makeFlightRecorderObjectName();
    Object o = connection.invoke(objectName, "newRecording", new Object[] {}, new String[] {});
    if (!(o instanceof Long)) {
      throw new RuntimeException("JMX returned something that wasn't a Long: " + o);
    }
    long recordingId = (Long) o;

    configureDefaultProfile(connection, recordingId);

    String maxAge = (harvestInterval.get(ChronoUnit.SECONDS) + 10) + "s";
    Map<String, String> options = new HashMap<>();
    options.put("name", "New Relic JFR Recording");
    options.put("disk", "true");
    options.put("maxAge", maxAge);

    // Have to pass this as actual open data, not as a Map
    String[] sig = new String[] {"long", "javax.management.openmbean.TabularData"};
    Object[] args = new Object[] {recordingId, makeOpenData(options)};
    connection.invoke(objectName, "setRecordingOptions", args, sig);

    // Now start the recording
    connection.invoke(
        objectName, "startRecording", new Object[] {recordingId}, new String[] {"long"});
    return recordingId;
  }

  private static void configureDefaultProfile(MBeanServerConnection connection, long recordingId)
      throws IOException, JMException {
    ObjectName objectName = makeFlightRecorderObjectName();
    connection.invoke(
        objectName,
        "setPredefinedConfiguration",
        new Object[] {recordingId, "profile"},
        new String[] {"long", "java.lang.String"});
  }

  static TabularDataSupport makeOpenData(Map<String, String> options) throws OpenDataException {
    String typeName = "java.util.Map<java.lang.String, java.lang.String>";
    String[] itemNames = new String[] {"key", "value"};
    OpenType<?>[] openTypes = new OpenType[] {SimpleType.STRING, SimpleType.STRING};
    CompositeType rowType = new CompositeType(typeName, typeName, itemNames, itemNames, openTypes);
    TabularType tabularType = new TabularType(typeName, typeName, rowType, new String[] {"key"});
    TabularDataSupport table = new TabularDataSupport(tabularType);

    for (Map.Entry<String, String> entry : options.entrySet()) {
      Object[] itemValues = {entry.getKey(), entry.getValue()};
      CompositeData element = new CompositeDataSupport(rowType, itemNames, itemValues);
      table.put(element);
    }

    return table;
  }

  static ObjectName makeFlightRecorderObjectName() throws MalformedObjectNameException {
    return new ObjectName("jdk.management.jfr:type=FlightRecorder");
  }
}
