package com.newrelic.jfr.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    // FIXME Counts are wrong, need to be adapted to the shape of the public file when this is re-enabled
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

  @Test
  void writeJsonSimple_noLimit() throws Exception {
    List<Map<String, String>> stack = new ArrayList<>();
    stack.add(buildFrame("action", "21", "77"));
    String payload = "{\"desc\":\"action\",\"line\":\"21\",\"bytecodeIndex\":\"77\"}";
    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":["
            + payload
            + "]}";
    var result = MethodSupport.jsonWrite(stack, Optional.empty());
    assertEquals(expected, result);
  }

  @Test
  void writeJsonSimple_limitMatchesFrameCount() throws Exception {
    List<Map<String, String>> stack = new ArrayList<>();
    stack.add(buildFrame("action", "21", "77"));
    String payload = "{\"desc\":\"action\",\"line\":\"21\",\"bytecodeIndex\":\"77\"}";
    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":["
            + payload
            + "]}";
    var result = MethodSupport.jsonWrite(stack, Optional.of(1));
    assertEquals(expected, result);
  }

  @Test
  void writeJsonSimple_withLimit() throws Exception {
    List<Map<String, String>> stack = new ArrayList<>();
    stack.add(buildFrame("action1", "21", "77"));
    stack.add(buildFrame("action2", "22", "78"));
    stack.add(buildFrame("action3", "23", "79"));
    String payload1 = "{\"desc\":\"action1\",\"line\":\"21\",\"bytecodeIndex\":\"77\"}";
    String payload2 = "{\"desc\":\"action2\",\"line\":\"22\",\"bytecodeIndex\":\"78\"}";
    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":true,\"payload\":["
            + payload1
            + ","
            + payload2
            + "]}";
    var result = MethodSupport.jsonWrite(stack, Optional.of(2));
    assertEquals(expected, result);
  }

  @Test
  void writeLargeStack() throws Exception {
    List<Map<String, String>> stack = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      stack.add(buildFrame(i));
    }

    String payloads =
        IntStream.range(0, 55)
            .mapToObj(
                i ->
                    "{\"desc\":\"action"
                        + i
                        + "\",\"line\":\""
                        + (21 + i)
                        + "\",\"bytecodeIndex\":\""
                        + (77 + i)
                        + "\"}")
            .collect(Collectors.joining(","));
    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":true,\"payload\":["
            + payloads
            + "]}";
    var result = MethodSupport.jsonWrite(stack, Optional.empty());
    assertEquals(expected, result);
  }

  @Test
  void writeLargeStack_edgeCase() throws Exception {
    List<Map<String, String>> stack = new ArrayList<>();
    // Specially crafted artisanal length in order to exercise the edge case
    // It happens on the second recursion.
    for (int i = 0; i < 75; i++) {
      var frame = Map.of("desc", "", "line", "", "bytecodeIndex", "");
      stack.add(frame);
    }

    String result = MethodSupport.jsonWrite(stack, Optional.of(74));
    assertNotNull(result);
    assertTrue(result.length() < 3 * 1024);
  }

  private Map<String, String> buildFrame(int i) {
    return buildFrame("action" + i, "" + (21 + i), "" + (77 + i));
  }

  private Map<String, String> buildFrame(String desc, String line, String bytecodeIndex) {
    return Map.of("desc", desc, "line", line, "bytecodeIndex", bytecodeIndex);
  }
}
