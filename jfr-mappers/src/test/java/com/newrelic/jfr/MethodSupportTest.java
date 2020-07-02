package com.newrelic.jfr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MethodSupportTest {

  private RecordingFile loadFile(String fName) throws IOException, URISyntaxException {
    URL url = MethodSupportTest.class.getClassLoader().getResource(fName);
    return new RecordingFile(Paths.get(url.toURI()));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testJsonWriteWithTruncate() throws Exception {
    var rf = new HashMap<String, String>();
    rf.put("desc", "Foo.meth:()V");
    rf.put("bytecodeIndex", "14");

    var frames = new ArrayList<Map<String, String>>();
    for (int i = 0; i < 8; i++) {
      rf.put("line", "" + (i + 10));
      frames.add((Map<String, String>) rf.clone());
    }

    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":true,\"payload\":[{\"desc\":\"Foo.meth:()V\",\"line\":\"10\",\"bytecodeIndex\":\"14\"},{\"desc\":\"Foo.meth:()V\",\"line\":\"11\",\"bytecodeIndex\":\"14\"},{\"desc\":\"Foo.meth:()V\",\"line\":\"12\",\"bytecodeIndex\":\"14\"}]}";
    var result = MethodSupport.jsonWrite(frames, Optional.of(3));
    assertEquals(expected, result);
  }

  @Test
  @Disabled
  public void simple_encode() throws Exception {
    var count = 0;
    var max = 0;
    var histo = new int[32];
    try (var recordingFile = loadFile("need-file.jfr")) {
      LOOP:
      while (recordingFile.hasMoreEvents()) {
        var event = recordingFile.readEvent();
        if (event != null) {
          var eventType = event.getEventType().getName();
          if (eventType.equals("jdk.ExecutionSample")
              || eventType.equals("jdk.NativeMethodSample")) {
            var trace = event.getStackTrace();
            var b64 = MethodSupport.serialize(trace);
            // FIXME Base-64 is not currently supported, use unencoded instead
            assertTrue(b64.startsWith("eyJ0eXBlIjoic3RhY2t0cmFjZSIsImxhbmd1YWdlIjoiamF2Y"));
            count = count + 1;
            if (max < b64.length()) {
              max = b64.length();
            }
            var bucket = b64.length() / 1000;
            histo[bucket] = histo[bucket] + 1;
          }
        }
      }
    }
    // FIXME Counts are wrong, need to be adapted to the shape of the public file when this is
    // re-enabled
    assertEquals(218684, count);
    assertEquals(16860, max);
    assertEquals(159, histo[0]);
    assertEquals(65435, histo[1]);
    assertEquals(116643, histo[2]);
    assertEquals(24899, histo[3]); // 94.72% fit under the 4k limit
    assertEquals(6303, histo[4]);
    assertEquals(3994, histo[5]);
    assertEquals(766, histo[6]);
    assertEquals(195, histo[7]); // 99.87% fit in 2 * 4k
  }
}
