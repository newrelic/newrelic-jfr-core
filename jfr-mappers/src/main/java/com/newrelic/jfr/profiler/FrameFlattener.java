package com.newrelic.jfr.profiler;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;

/**
 * This class flattens the root {@link FlamegraphMarshaller.StackFrame} object into a single list
 */
public class FrameFlattener {

  public List<StackFrame> flatten(FlamegraphMarshaller.StackFrame root) {
    return addChildren(root, null, new AtomicInteger(0));
  }

  private List<StackFrame> addChildren(
      FlamegraphMarshaller.StackFrame current, StackFrame parent, AtomicInteger idGenerator) {
    if (current == null) {
      return emptyList();
    }

    String parentId = parent == null ? null : parent.getId();
    String id = String.valueOf(idGenerator.incrementAndGet());
    StackFrame currentNewStackFrame = new StackFrame(current.getName(), current.getValue(), parentId, id);
    List<StackFrame> flattenedResult = new LinkedList<>();
    flattenedResult.add(currentNewStackFrame);

    if (current.getChildren() == null) {
      return flattenedResult;
    }

    for (FlamegraphMarshaller.StackFrame child : current.getChildren()) {
      flattenedResult.addAll(addChildren(child, currentNewStackFrame, idGenerator));
    }
    return flattenedResult;
  }
}
