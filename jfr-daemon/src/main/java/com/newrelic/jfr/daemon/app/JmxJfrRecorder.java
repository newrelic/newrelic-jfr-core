package com.newrelic.jfr.daemon.app;

import static com.newrelic.jfr.daemon.app.JmxJfrRecorderFactory.makeFlightRecorderObjectName;
import static com.newrelic.jfr.daemon.app.JmxJfrRecorderFactory.makeOpenData;

import com.newrelic.jfr.daemon.JfrRecorder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link JfrRecorder} records JFR data from a remote MBean Server over JMX.
 */
public class JmxJfrRecorder implements JfrRecorder {

  private static final Logger logger = LoggerFactory.getLogger(JmxJfrRecorder.class);
  private static final int MAX_BYTES_READ = 5 * 1024 * 1024;

  private final MBeanServerConnection connection;
  private final boolean streamFromJmx;
  private final long recordingId;

  public JmxJfrRecorder(MBeanServerConnection connection, boolean streamFromJmx, long recordingId) {
    this.connection = connection;
    this.streamFromJmx = streamFromJmx;
    this.recordingId = recordingId;
  }

  @Override
  public Path recordToFile() throws Exception {
    return streamFromJmx ? streamRecordingToFile() : copyRecordingToFile();
  }

  Path streamRecordingToFile() throws Exception {
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

  Path copyRecordingToFile() throws Exception {
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
