package com.newrelic.jfr.daemon;

import static javax.management.remote.JMXConnectorFactory.newJMXConnector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.management.*;
import javax.management.openmbean.*;
import javax.management.remote.JMXServiceURL;
import jdk.jfr.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JFRJMXRecorder {
  private static final Logger logger = LoggerFactory.getLogger(JFRJMXRecorder.class);

  private static final int MAX_BYTES_READ = 5 * 1024 * 1024;

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

  /**
   * Factory method for creating an instance of the JFRJMXRecorder.
   *
   * @param config - The daemon configuration instance
   * @return A newly created and connected JFRJMXRecorder
   * @throws IOException if connection fails
   */
  public static JFRJMXRecorder connect(DaemonConfig config) throws IOException {
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

  public void startRecording()
      throws MalformedObjectNameException, MBeanException, InstanceNotFoundException,
          ReflectionException, IOException, OpenDataException {
    logger.debug("In startRecording()");

    final var objectName = new ObjectName("jdk.management.jfr:type=FlightRecorder");
    var o = connection.invoke(objectName, "newRecording", new Object[] {}, new String[] {});
    if (!(o instanceof Long)) {
      throw new RuntimeException("JMX returned something that wasn't a Long: " + o);
    }
    recordingId = (Long) o;

    try {
      final String content = Configuration.getConfiguration("profile").getContents();
      connection.invoke(
          objectName,
          "setConfiguration",
          new Object[] {recordingId, content},
          new String[] {"long", "java.lang.String"});
    } catch (ParseException e) {
      // TODO: Something
    }

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

    var objectName = new ObjectName("jdk.management.jfr:type=FlightRecorder");
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
    var objectName = new ObjectName("jdk.management.jfr:type=FlightRecorder");

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
