package com.newrelic.jfr.profiler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Stack;
import org.junit.jupiter.api.Test;

public class FlamegraphMarshallerTest {

  @Test
  public void testAddFrame() {
    String expectedFrameName = "expectedFrameName";
    Integer expectedFrameValue = 17;

    FlamegraphMarshaller.StackFrame stackFrame = new FlamegraphMarshaller.StackFrame("stackFrame");
    FlamegraphMarshaller.StackFrame result =
        stackFrame.addFrame(expectedFrameName, expectedFrameValue);

    assertEquals(expectedFrameName, result.getName());
    assertEquals(expectedFrameValue, result.getValue());
    assertNull(result.getChildren());
    assertTrue(stackFrame.getChildren().contains(result));
  }

  @Test
  public void testProcessEvent() {
    var stack = new Stack<String>();

    String expectedFrameName1 = "expectedFrameName1";
    String expectedFrameName2 = "expectedFrameName2";
    String expectedFrameName3 = "expectedFrameName3";
    Integer expectedFrameValue = 27;

    stack.push(expectedFrameName1);
    stack.push(expectedFrameName2);
    stack.push(expectedFrameName3);

    FlamegraphMarshaller flamegraphMarshaller = new FlamegraphMarshaller();
    flamegraphMarshaller.processEvent(stack, expectedFrameValue);
    FlamegraphMarshaller.StackFrame stackFrame = flamegraphMarshaller.getStackFrame();
    List<FlamegraphMarshaller.StackFrame> children = stackFrame.getChildren();
    assertNotNull(children);

    FlamegraphMarshaller.StackFrame frame3 =
        (FlamegraphMarshaller.StackFrame) children.toArray()[0];
    FlamegraphMarshaller.StackFrame frame2 =
        (FlamegraphMarshaller.StackFrame) frame3.getChildren().toArray()[0];
    FlamegraphMarshaller.StackFrame frame1 =
        (FlamegraphMarshaller.StackFrame) frame2.getChildren().toArray()[0];

    assertEquals(expectedFrameName3, frame3.getName());
    assertEquals(expectedFrameValue, frame3.getValue());

    assertEquals(expectedFrameName2, frame2.getName());
    assertEquals(expectedFrameValue, frame2.getValue());

    assertEquals(expectedFrameName1, frame1.getName());
    assertEquals(expectedFrameValue, frame1.getValue());
  }
}
