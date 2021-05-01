/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newrelic.jfr.profiler;

import static java.util.Collections.emptyList;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Some code originally based upon a version of the JsonOutputWriter found at
 * https://github.com/chrishantha/jfr-flame-graph - it creates JSON output to be used with
 * d3-flame-graph. https://github.com/spiermar/d3-flame-graph
 */
public class FlamegraphMarshaller {

  /** The bottom of the stack must be "root" */
  private static final String ROOT = "root";

  /** The data model for json */
  private StackFrame profile = new StackFrame(ROOT);

  public static class StackFrame {
    private String name;
    private Integer value = 0;
    private List<StackFrame> children = null;
    private Map<String, StackFrame> childrenMap = new HashMap<>();

    public StackFrame(String name) {
      this.name = name;
    }

    public StackFrame addFrame(String frameName, Integer size) {
      if (children == null) {
        children = new ArrayList<>();
      }
      StackFrame frame = childrenMap.get(frameName);
      if (frame == null) {
        frame = new StackFrame(frameName);
        childrenMap.put(frameName, frame);
        children.add(frame);
      }
      frame.value += size;
      return frame;
    }

    public String getName() {
      return name;
    }

    public Integer getValue() {
      return value;
    }

    public List<StackFrame> getChildren() {
      return children;
    }

    public String toString() {
      Gson gson = new Gson();
      return gson.toJson(this);
    }
  }

  public List<FlameLevel> flatten(FlamegraphMarshaller.StackFrame root) {
    return addChildren(root, null, new AtomicInteger(0));
  }

  private List<FlameLevel> addChildren(
      FlamegraphMarshaller.StackFrame current, FlameLevel parent, AtomicInteger idGenerator) {
    if (current == null) {
      return emptyList();
    }

    String parentId = parent == null ? null : parent.getId();
    String id = String.valueOf(idGenerator.incrementAndGet());
    FlameLevel currentNewFlameLevel =
        new FlameLevel(current.getName(), current.getValue(), parentId, id);
    List<FlameLevel> flattenedResult = new LinkedList<>();
    flattenedResult.add(currentNewFlameLevel);

    if (current.getChildren() == null) {
      return flattenedResult;
    }

    for (FlamegraphMarshaller.StackFrame child : current.getChildren()) {
      flattenedResult.addAll(addChildren(child, currentNewFlameLevel, idGenerator));
    }
    return flattenedResult;
  }

  public void processEvent(Stack<String> stack, Integer size) {
    StackFrame frame = profile;
    frame.value += size;

    while (!stack.empty()) {
      frame = frame.addFrame(stack.pop(), size);
    }
  }

  public StackFrame getStackFrame() {
    return this.profile;
  }

  public void writeOutput(BufferedWriter bufferedWriter) throws IOException {
    Gson gson = new Gson();
    gson.toJson(this.profile, bufferedWriter);
  }

  @Override
  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this.profile);
  }
}
