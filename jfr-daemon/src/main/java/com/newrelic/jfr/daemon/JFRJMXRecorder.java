/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static javax.management.remote.JMXConnectorFactory.newJMXConnector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.management.*;
import javax.management.openmbean.*;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JFRJMXRecorder {
  private static final Logger logger = LoggerFactory.getLogger(JFRJMXRecorder.class);

  private static final int MAX_BYTES_READ = 5 * 1024 * 1024;
  private static final List<Integer> BACKOFF_SECONDS = List.of(1, 2, 4, 8, 15);

  private final MBeanServerConnection connection;
  private final Duration harvestCycleDuration;
  private final boolean streamFromJmx;

  private long recordingId;

  public JFRJMXRecorder(
      MBeanServerConnection connection, Duration harvestInterval, boolean streamFromJmx) {
    this.connection = connection;
    this.harvestCycleDuration = harvestInterval;
    this.streamFromJmx = streamFromJmx;
  }

  public static JFRJMXRecorder connectWithBackOff(DaemonConfig config) throws IOException {
    return connectWithBackOff(config, 0);
  }

  private static JFRJMXRecorder connectWithBackOff(DaemonConfig config, int backoffIndex)
      throws IOException {
    try {
      return connect(config);
    } catch (IOException e) {
      if (backoffIndex >= BACKOFF_SECONDS.size() - 1) {
        logger.error("Failed to connect to JMX and retries exhausted.  JFR will be disabled.", e);
        throw e;
      }
      logger.warn("Error connecting to JMX, backing off before retry");
      sleepSafely(backoffIndex);
      return connectWithBackOff(config, backoffIndex + 1);
    }
  }

  public void startRecordingWithBackOff() throws JMException, IOException {
    startRecordingWithBackOff(0);
  }

  public void startRecordingWithBackOff(int backoffIndex) throws JMException, IOException {
    try {
      startRecording();
    } catch (Exception e) {
      if (backoffIndex >= BACKOFF_SECONDS.size() - 1) {
        logger.error("Failed to start recording and retries exhausted.  JFR will be disabled.", e);
        throw e;
      }
      logger.warn("Error starting recording, backing off before retry", e);
      sleepSafely(backoffIndex);
      startRecordingWithBackOff(backoffIndex + 1);
    }
  }

  private static void sleepSafely(int backoffIndex) {
    try {
      TimeUnit.SECONDS.sleep(BACKOFF_SECONDS.get(backoffIndex));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Error during backoff sleep", e);
    }
  }

  /**
   * Factory method for creating an instance of the JFRJMXRecorder.
   *
   * @param config - The daemon configuration instance
   * @return A newly created and connected JFRJMXRecorder
   * @throws IOException if connection fails
   */
  static JFRJMXRecorder connect(DaemonConfig config) throws IOException {
    //        var map = new HashMap<String, Object>();
    //        var credentials = new String[]{"", ""};
    //        map.put("jmx.remote.credentials", credentials);
    var urlPath = "/jndi/rmi://" + config.getJmxHost() + ":" + config.getJmxPort() + "/jmxrmi";
    var url = new JMXServiceURL("rmi", "", 0, urlPath);
    var connector = newJMXConnector(url, null);
    connector.connect();
    var connection = connector.getMBeanServerConnection();
    return new JFRJMXRecorder(connection, config.getHarvestInterval(), config.streamFromJmx());
  }

  void startRecording() throws JMException, IOException {
    logger.debug("In startRecording()");

    ObjectName objectName = makeFlightRecorderObjectName();
    var o = connection.invoke(objectName, "newRecording", new Object[] {}, new String[] {});
    if (!(o instanceof Long)) {
      throw new RuntimeException("JMX returned something that wasn't a Long: " + o);
    }
    recordingId = (Long) o;

    configureDefaultProfile();

    var maxAge = (harvestCycleDuration.toSeconds() + 10) + "s";
    Map<String, String> options = new HashMap<>();
    options.put("name", "New Relic JFR Recording");
    options.put("disk", "true");
    options.put("maxAge", maxAge);

    // Have to pass this as actual open data, not as a Map
    var sig = new String[] {"long", "javax.management.openmbean.TabularData"};
    var args = new Object[] {recordingId, makeOpenData(options)};
    connection.invoke(objectName, "setRecordingOptions", args, sig);

    // Now start the recording
    connection.invoke(
        objectName, "startRecording", new Object[] {recordingId}, new String[] {"long"});
  }

  private void configureDefaultProfile() throws IOException, JMException {
    ObjectName objectName = makeFlightRecorderObjectName();
    connection.invoke(
        objectName,
        "setPredefinedConfiguration",
        new Object[] {recordingId, "profile"},
        new String[] {"long", "java.lang.String"});
  }

  private ObjectName makeFlightRecorderObjectName() throws MalformedObjectNameException {
    return new ObjectName("jdk.management.jfr:type=FlightRecorder");
  }

  /**
   * Stores JFR recording data in a local file on disk. The data is either streamed over JMX or
   * copied from a shared filesystem.
   *
   * @return Path to local file on disc
   * @throws MalformedObjectNameException JMX problem with the objectname
   * @throws ReflectionException remove invocation failed due to reflection
   * @throws MBeanException yet another MBean exception
   * @throws InstanceNotFoundException Couldn't find the instance to invoke
   * @throws IOException Generic input/output exception
   * @throws OpenDataException problems creating instances of JMX objects
   */
  public Path recordToFile()
      throws MalformedObjectNameException, ReflectionException, MBeanException,
          InstanceNotFoundException, IOException, OpenDataException {
    return streamFromJmx ? streamRecordingToFile() : copyRecordingToFile();
  }

  /**
   * Retrieves the JFR recording over the network and stores it in a file on local disk
   *
   * @return Path to local file on disc
   * @throws MalformedObjectNameException JMX problem with the objectname
   * @throws ReflectionException remove invocation failed due to reflection
   * @throws MBeanException yet another MBean exception
   * @throws InstanceNotFoundException Couldn't find the instance to invoke
   * @throws IOException Generic input/output exception
   * @throws OpenDataException problems creating instances of JMX objects
   */
  Path streamRecordingToFile()
      throws MalformedObjectNameException, ReflectionException, MBeanException,
          InstanceNotFoundException, IOException, OpenDataException {

    ObjectName objectName = makeFlightRecorderObjectName();
    var oClone =
        connection.invoke(
            objectName,
            "cloneRecording",
            new Object[] {recordingId, true},
            new String[] {"long", "boolean"});
    if (!(oClone instanceof Long)) {
      throw new RuntimeException("JMX returned something that wasn't a Long: " + oClone);
    }
    var cloneId = (Long) oClone;

    var streamOptions = new HashMap<String, String>();
    streamOptions.put("blockSize", "" + MAX_BYTES_READ);

    var sig = new String[] {"long", "javax.management.openmbean.TabularData"};
    Object[] args = new Object[] {cloneId, makeOpenData(streamOptions)};

    var oStream = connection.invoke(objectName, "openStream", args, sig);
    if (!(oStream instanceof Long)) {
      throw new RuntimeException("JMX returned something that wasn't a Long: " + oStream);
    }
    var streamId = (Long) oStream;
    var moreToRead = true;

    var dir = Files.createTempDirectory("nr-jfr");
    var file = Files.createTempFile(dir, "stream-" + System.currentTimeMillis(), null);

    try (var outputStream = Files.newOutputStream(file)) {
      logger.debug("Opening stream from target process");
      while (moreToRead) {
        var oBytes =
            connection.invoke(
                objectName, "readStream", new Object[] {streamId}, new String[] {"long"});
        if (oBytes == null) {
          break;
        }
        if (!(oBytes instanceof byte[])) {
          throw new RuntimeException("JMX returned something that wasn't a byte array: " + oBytes);
        }
        var bytesRead = (byte[]) oBytes;
        logger.debug("Reading bytes from stream: " + bytesRead.length);
        if (bytesRead.length < MAX_BYTES_READ) {
          moreToRead = false;
        }
        outputStream.write(bytesRead);
      }
    }

    logger.debug("Closing JMX stream");
    connection.invoke(objectName, "closeStream", new Object[] {streamId}, new String[] {"long"});

    connection.invoke(objectName, "closeRecording", new Object[] {cloneId}, new String[] {"long"});

    return file;
  }

  private TabularDataSupport makeOpenData(final Map<String, String> options)
      throws OpenDataException {
    var typeName = "java.util.Map<java.lang.String, java.lang.String>";
    var itemNames = new String[] {"key", "value"};
    var openTypes = new OpenType[] {SimpleType.STRING, SimpleType.STRING};
    var rowType = new CompositeType(typeName, typeName, itemNames, itemNames, openTypes);
    var tabularType = new TabularType(typeName, typeName, rowType, new String[] {"key"});
    var table = new TabularDataSupport(tabularType);

    for (var entry : options.entrySet()) {
      Object[] itemValues = {entry.getKey(), entry.getValue()};
      CompositeData element = new CompositeDataSupport(rowType, itemNames, itemValues);
      table.put(element);
    }

    return table;
  }

  /**
   * Requires the JMX process to share a local filesystem with the target.
   *
   * @return Path to local file on disc
   * @throws MalformedObjectNameException JMX problem with the objectname
   * @throws ReflectionException remove invocation failed due to reflection
   * @throws MBeanException yet another MBean exception
   * @throws InstanceNotFoundException Couldn't find the instance to invoke
   * @throws IOException Generic input/output exception
   */
  Path copyRecordingToFile()
      throws MalformedObjectNameException, MBeanException, InstanceNotFoundException,
          ReflectionException, IOException {
    ObjectName objectName = makeFlightRecorderObjectName();

    var oClone =
        connection.invoke(
            objectName,
            "cloneRecording",
            new Object[] {recordingId, true},
            new String[] {"long", "boolean"});
    if (!(oClone instanceof Long)) {
      throw new RuntimeException("JMX returned something that wasn't a Long: " + oClone);
    }
    var cloneId = (Long) oClone;

    var dir = Files.createTempDirectory("nr-jfr");
    var file = Files.createTempFile(dir, "stream-" + System.currentTimeMillis(), null);

    connection.invoke(
        objectName,
        "copyTo",
        new Object[] {cloneId, file.toString()},
        new String[] {"long", "java.lang.String"});

    connection.invoke(objectName, "closeRecording", new Object[] {cloneId}, new String[] {"long"});

    // FIXME Shutdown hooks for deleting file in the case of unclean stop

    return file;
  }
}
