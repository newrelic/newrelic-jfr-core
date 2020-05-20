package com.newrelic.jfr.stacktrace;

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import jdk.jfr.consumer.RecordedStackTrace;

public class StackTraceBlob {
  public static String encodeB64(RecordedStackTrace trace) {
    var payload = new ArrayList<Map<String, String>>();
    var frames = trace.getFrames();
    for (int i = 0; i < frames.size(); i++) {
      var jsonObj = new HashMap<String, String>();
      var frame = frames.get(i);
      var method = frame.getMethod();
      var methodDesc = method.getType().getName() + "." + method.getName() + method.getDescriptor();
      jsonObj.put("desc", methodDesc);
      jsonObj.put("line", "" + frame.getLineNumber());
      jsonObj.put("bytecodeIndex", "" + frame.getBytecodeIndex());
      payload.add(jsonObj);
    }

    // This API is nasty
    String out = null;
    try {
      out = new String(Base64.getEncoder().encode(jsonEncode(payload).getBytes()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate stacktrace json", e);
    }
    return out;
  }

  static String jsonEncode(List<Map<String, String>> frames) throws IOException {
    StringWriter out = new StringWriter();
    JsonWriter jsonWriter = new JsonWriter(out);
    jsonWriter.beginObject();
    jsonWriter.name("type").value("stacktrace");
    jsonWriter.name("language").value("java");
    jsonWriter.name("value").value(1);
    jsonWriter.name("payload").beginArray();
    for (final var frame : frames) {
      jsonWriter.beginObject();
      jsonWriter.name("desc").value(frame.get("desc"));
      jsonWriter.name("line").value(frame.get("line"));
      jsonWriter.name("bytecodeIndex").value(frame.get("bytecodeIndex"));
      jsonWriter.endObject();
    }

    jsonWriter.endArray();
    jsonWriter.endObject();
    return out.toString();
  }

  static String jsonEncodeDummy(List<Map<String, String>> jsonObj) {
    // Ensure the key ordering is always type, language, version, payload
    // as this ensures that base-64 encoded versions can be compared
    return "{\"type\":\"stacktrace\", \"language\":\"java\", \"version\":1, \"payload\":[ ... frames .... ], \"metadata\":{}}";
  }
}
