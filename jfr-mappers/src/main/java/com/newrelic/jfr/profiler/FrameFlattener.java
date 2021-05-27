package com.newrelic.jfr.profiler;

import static java.util.Collections.emptyList;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class flattens the root {@link FlamegraphMarshaller.StackFrame} object into a single list
 */
public class FrameFlattener {

  public List<FlameLevel> flatten(FlamegraphMarshaller.StackFrame stackFrame) {
    List<FlameLevel> frames = addChildren(stackFrame, null, new AtomicInteger(0));
    return frames;
  }

  private List<FlameLevel> addChildren(
      FlamegraphMarshaller.StackFrame current, FlameLevel parent, AtomicInteger idGenerator) {
    if (current == null) {
      return emptyList();
    }

    // parentId is set to "null" string because null type will cause an NPE in mapToJson in
    // EventBatchMarshaller of TelemetryClient.
    String parentId = parent == null ? "null" : parent.getId();
    String id = current.getName();
    FlameLevel currentNewFlameLevel =
        new FlameLevel(current.getName(), current.getValue(), parentId, id);
    LinkedList<FlameLevel> flattenedResult = new LinkedList<>();
    flattenedResult.add(currentNewFlameLevel);

    if (current.getChildren() == null) {
      return flattenedResult;
    }

    for (FlamegraphMarshaller.StackFrame child : current.getChildren()) {
      flattenedResult.addAll(addChildren(child, currentNewFlameLevel, idGenerator));
    }
    return flattenedResult;
  }
}
