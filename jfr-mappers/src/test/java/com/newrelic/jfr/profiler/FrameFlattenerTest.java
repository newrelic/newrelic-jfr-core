package com.newrelic.jfr.profiler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FrameFlattenerTest {
  @Test
  public void testNullRoot() {
    var testClass = new FlamegraphMarshaller();
    var result = testClass.flatten(null);
    List<FlameLevel> expected = Collections.emptyList();
    assertEquals(expected, result);
  }

  @Test
  public void testNullChildren() {
    var testClass = new FlamegraphMarshaller();
    var stackFrame = new FlamegraphMarshaller.StackFrame("testFrame");

    var result = testClass.flatten(stackFrame);
    var expected = new FlameLevel("testFrame", 0, null, String.valueOf(1));

    assertEquals(expected, result.get(0));
  }

  @Test
  public void testFlattenHappyPath() {
    var testClass = new FlamegraphMarshaller();
    var stackFrame = new FlamegraphMarshaller.StackFrame("root");
    stackFrame.addFrame("child1", 1).addFrame("child2", 10);
    stackFrame.addFrame("child3", 1);
    var result = testClass.flatten(stackFrame);

    var expectedRoot = new FlameLevel("root", 0, null, String.valueOf(1));
    var expectedChild1 = new FlameLevel("child1", 1, String.valueOf(1), String.valueOf(2));
    var expectedChild2 = new FlameLevel("child2", 10, String.valueOf(2), String.valueOf(3));
    var expectedChild3 = new FlameLevel("child3", 1, String.valueOf(1), String.valueOf(4));

    assertEquals(4, result.size());
    assertEquals(expectedRoot, result.get(0));
    assertEquals(expectedChild1, result.get(1));
    assertEquals(expectedChild2, result.get(2));
    assertEquals(expectedChild3, result.get(3));
  }

  @Test
  public void testChildrenBreadth() {
    var testClass = new FlamegraphMarshaller();
    var stackFrame = new FlamegraphMarshaller.StackFrame("root");
    for (int i = 1; i < 72000; i++) {
      stackFrame.addFrame(String.valueOf(i), 1);
    }
    var result = testClass.flatten(stackFrame);
    assertEquals(72000, result.size());

    int count = 0;
    for (FlameLevel s : result) {
      if (s.getParentId() != null) {
        assertEquals(String.valueOf(1), s.getParentId());
      } else {
        count++;
      }
    }
    // There can be only one parent
    assertEquals(1, count);
  }

  @Test
  public void testChildrenDepth() {
    var testClass = new FlamegraphMarshaller();
    var stackFrame = new FlamegraphMarshaller.StackFrame("root");
    makeStackFrames(stackFrame, 99);

    var result = testClass.flatten(stackFrame);
    var parentIds = new HashSet<>();
    for (FlameLevel s : result) {
      if (!parentIds.add(s.getParentId())) {
        fail("ParentIds should be unique " + s.getParentId());
      }
    }
    assertEquals(100, parentIds.size());
  }

  private void makeStackFrames(FlamegraphMarshaller.StackFrame stackFrame, int count) {
    if (count > 0) {
      var frame = stackFrame.addFrame("name", 1);
      makeStackFrames(frame, --count);
    }
  }
}
