package com.newrelic.jfr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;

public class MethodSupportTest {

  private RecordingFile loadFile(String fName) throws IOException, URISyntaxException {
    URL url = MethodSupportTest.class.getClassLoader().getResource(fName);
    return new RecordingFile(Paths.get(url.toURI()));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testJsonWriteWithTruncate() throws Exception {
    var method = buildMethod("Foo", "meth", "()V");
    
    var frames = new ArrayList<RecordedFrame>();
    for (int i = 0; i < 8; i++) {
      var frame = buildFrame(method, i + 10, 14);
      frames.add(frame);
    }

    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":true,\"payload\":[{\"desc\":\"Foo.meth()V\",\"line\":\"10\",\"bytecodeIndex\":\"14\"},{\"desc\":\"Foo.meth()V\",\"line\":\"11\",\"bytecodeIndex\":\"14\"},{\"desc\":\"Foo.meth()V\",\"line\":\"12\",\"bytecodeIndex\":\"14\"}]}";
    var result = MethodSupport.jsonWrite(frames, Optional.of(3));
    assertEquals(expected, result);
  }

  @Test
  public void simple_encode() throws Exception {
    var count = 0;
    var max = 0;
    var histo = new int[32];
    try (var recordingFile = loadFile("startup3.jfr")) {
      LOOP:
      while (recordingFile.hasMoreEvents()) {
        var event = recordingFile.readEvent();
        if (event != null) {
          var eventType = event.getEventType().getName();
          if (eventType.equals("jdk.ExecutionSample")
              || eventType.equals("jdk.NativeMethodSample")) {
            var trace = event.getStackTrace();
            var b64 = MethodSupport.serialize(trace);
            assertTrue(
                b64.startsWith(
                    "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":"));
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

    assertEquals(3113, count);
    assertEquals(2986, max);
    assertEquals(1627, histo[0]);
    assertEquals(1484, histo[1]);
    assertEquals(2, histo[2]);
    assertEquals(0, histo[3]); // 94.72% fit under the 4k limit
    assertEquals(0, histo[4]);
    assertEquals(0, histo[5]);
    assertEquals(0, histo[6]);
    assertEquals(0, histo[7]); // 99.87% fit in 2 * 4k
  }

  @Test
  void writeJsonSimple_noLimit() throws Exception {
    var action = buildMethod("Act", "ion", "");
    List<RecordedFrame> stack = new ArrayList<>();
    stack.add(buildFrame(action, 21, 77));
    String payload = "{\"desc\":\"Act.ion\",\"line\":\"21\",\"bytecodeIndex\":\"77\"}";
    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":["
            + payload
            + "]}";
    var result = MethodSupport.jsonWrite(stack, Optional.empty());
    assertEquals(expected, result);
  }

  @Test
  void writeJsonSimple_limitMatchesFrameCount() throws Exception {
    var action = buildMethod("Act", "ion", "()V");
    List<RecordedFrame> stack = new ArrayList<>();
    stack.add(buildFrame(action, 21, 77));
    String payload = "{\"desc\":\"Act.ion()V\",\"line\":\"21\",\"bytecodeIndex\":\"77\"}";
    var expected =
        "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":["
            + payload
            + "]}";
    var result = MethodSupport.jsonWrite(stack, Optional.of(1));
    assertEquals(expected, result);
  }

  @Test
  void writeJsonSimple_withLimit() throws Exception {
    var action1 = buildMethod("Foo", "action1", "()V");
    var action2 = buildMethod("Foo", "action2", "()V");
    var action3 = buildMethod("Foo", "action3", "()V");

    List<RecordedFrame> stack = new ArrayList<>();
    stack.add(buildFrame(action1, 21, 77));
    stack.add(buildFrame(action2, 22, 78));
    stack.add(buildFrame(action3, 23, 79));
    String payload1 = "{\"desc\":\"Foo.action1()V\",\"line\":\"21\",\"bytecodeIndex\":\"77\"}";
    String payload2 = "{\"desc\":\"Foo.action2()V\",\"line\":\"22\",\"bytecodeIndex\":\"78\"}";
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
    List<RecordedFrame> stack = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      var method = buildMethod("Act", "io" + i, "");
      stack.add(buildFrame(method, 21 + i, 77 + i));
    }

    String payloads =
        IntStream.range(0, 55)
            .mapToObj(
                i ->
                    "{\"desc\":\"Act.io" + i
                        + "\",\"line\":\"" + (21 + i)
                        + "\",\"bytecodeIndex\":\"" + (77 + i)
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
    var stack = new ArrayList<RecordedFrame>();
    var method = buildMethod("", "", "");
    // Specially crafted artisanal length in order to exercise the edge case
    // It happens on the second recursion.
    for (int i = 0; i < 75; i++) {
      var frame = buildFrame(method, 0, 0);
      stack.add(frame);
    }

    String result = MethodSupport.jsonWrite(stack, Optional.of(74));
    assertNotNull(result);
    assertTrue(result.length() <= MethodSupport.HEADROOM_75PC);
  }

  private RecordedMethod buildMethod(String typeName, String methodName, String descriptor) {
    RecordedMethod method = mock(RecordedMethod.class, RETURNS_DEEP_STUBS);
    when(method.getType().getName()).thenReturn(typeName);
    when(method.getName()).thenReturn(methodName);
    when(method.getDescriptor()).thenReturn(descriptor);
    return method;
  }

  private RecordedFrame buildFrame(RecordedMethod method, int line, int bytecodeIndex) {
    RecordedFrame frame = mock(RecordedFrame.class);
    when(frame.getBytecodeIndex()).thenReturn(bytecodeIndex);
    when(frame.getLineNumber()).thenReturn(line);
    when(frame.getMethod()).thenReturn(method);
    return frame;
  }

//  private Map<String, String> buildFrame(int i) {
//    return buildFrame("action" + i, "" + (21 + i), "" + (77 + i));
//  }
//
//  private RecordedFrame buildFrame(String desc, String line, String bytecodeIndex) {
//
//    return Map.of("desc", desc, "line", line, "bytecodeIndex", bytecodeIndex);
//  }

}
