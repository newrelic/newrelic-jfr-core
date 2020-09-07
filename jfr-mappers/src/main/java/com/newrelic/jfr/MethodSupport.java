/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr;

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedStackTrace;

public final class MethodSupport {
  private static final int JSON_SCHEMA_VERSION = 1;
  private static final int HEADROOM_75PC = 3 * 1024;

  public static String describeMethod(final RecordedMethod method) {
    if (method == null) {
      return "[missing]";
    }

    return method.getType().getName() + "." + method.getName() + method.getDescriptor();
  }

  public static String serialize(final RecordedStackTrace trace) {
    var payload = new ArrayList<Map<String, String>>();
    var frames = trace.getFrames();
    for (int i = 0; i < frames.size(); i++) {
      var frameData = new HashMap<String, String>();
      var frame = frames.get(i);
      frameData.put("desc", describeMethod(frame.getMethod()));
      frameData.put("line", "" + frame.getLineNumber());
      frameData.put("bytecodeIndex", "" + frame.getBytecodeIndex());
      payload.add(frameData);
    }

    try {
      return new String(jsonWrite(payload, Optional.empty()).getBytes());
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate stacktrace json", e);
    }
  }

  static String jsonWrite(final List<Map<String, String>> frames, final Optional<Integer> limit)
      throws IOException {
    var strOut = new StringWriter();
    var jsonWriter = new JsonWriter(strOut);
    var frameCount = Math.min(limit.orElse(frames.size()), frames.size());

    jsonWriter.beginObject();
    jsonWriter.name("type").value("stacktrace");
    jsonWriter.name("language").value("java");
    jsonWriter.name("version").value(JSON_SCHEMA_VERSION);
    jsonWriter.name("truncated").value(frameCount < frames.size());
    jsonWriter.name("payload").beginArray();
    for (int i = 0; i < frameCount; i++) {
      var frame = frames.get(i);
      jsonWriter.beginObject();
      jsonWriter.name("desc").value(frame.get("desc"));
      jsonWriter.name("line").value(frame.get("line"));
      jsonWriter.name("bytecodeIndex").value(frame.get("bytecodeIndex"));
      jsonWriter.endObject();
    }

    jsonWriter.endArray();
    jsonWriter.endObject();
    var out = strOut.toString();
    var length = out.length();
    if (length > HEADROOM_75PC) {
      // Truncate the stack frame and try again
      double percentageOfFramesToTry = ((double) HEADROOM_75PC) / length;
      int numFrames = (int) (frameCount * percentageOfFramesToTry);
      if (numFrames < frameCount) {
        return jsonWrite(frames, Optional.of(numFrames));
      }
      throw new IOException(
          "Corner case of a stack frame that can't be cleanly truncated! "
              + "numFrames = "
              + numFrames
              + ", frameCount = "
              + frameCount
              + ", "
              + ", percentageOfFramesToTry = "
              + percentageOfFramesToTry
              + ", length = "
              + length);
    } else {
      return out;
    }
  }
}
