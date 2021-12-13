package com.newrelic.jfr.profiler;

import static com.newrelic.jfr.RecordedObjectValidators.hasField;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.jfr.profiler.FlamegraphMarshaller.StackFrame;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

public class ProfileSummarizer implements EventToEventSummary {
  public static final String EVENT_NAME = "jdk.ExecutionSample";
  public static final String NATIVE_EVENT_NAME = "jdk.NativeMethodSample";
  public static final String SIMPLE_CLASS_NAME = ProfileSummarizer.class.getSimpleName();
  public static final String STATE = "state";
  public static final String THREAD_NAME = "thread.name";
  public static final String SAMPLED_THREAD = "sampledThread";
  public static final String JFR_FLAMELEVEL = "JfrFlameLevel";
  public static final String FLAME_NAME = "flamelevel.name";
  public static final String FLAME_VALUE = "flamelevel.value";
  public static final String FLAME_PARENT_ID = "flamelevel.parentId";
  private final FrameFlattener flattener;
  private final ThreadNameNormalizer nameNormalizer;

  private final String eventName;

  private final Map<String, List<JvmStackTraceEvent>> stackTraceEventPerThread = new HashMap<>();
  private AtomicLong timestamp = new AtomicLong(Long.MAX_VALUE);

  // For tests
  public Map<String, List<JvmStackTraceEvent>> getStackTraceEventPerThread() {
    return stackTraceEventPerThread;
  }

  private ProfileSummarizer(
      final String eventName, FrameFlattener frameFlattener, ThreadNameNormalizer nameNormalizer) {
    this.eventName = eventName;
    this.flattener = frameFlattener;
    this.nameNormalizer = nameNormalizer;
  }

  public static ProfileSummarizer forExecutionSample(ThreadNameNormalizer nameNormalizer) {
    return new ProfileSummarizer(EVENT_NAME, new FrameFlattener(), nameNormalizer);
  }

  public static ProfileSummarizer forNativeMethodSample(ThreadNameNormalizer nameNormalizer) {
    return new ProfileSummarizer(NATIVE_EVENT_NAME, new FrameFlattener(), nameNormalizer);
  }

  @Override
  public String getEventName() {
    return eventName;
  }

  @Override
  public void accept(RecordedEvent ev) {
    RecordedStackTrace trace = ev.getStackTrace();
    if (trace == null) {
      return;
    }
    timestamp.updateAndGet(current -> Math.min(current, ev.getStartTime().toEpochMilli()));

    String threadName = null;
    if (hasField(ev, SAMPLED_THREAD, SIMPLE_CLASS_NAME)) {
      RecordedThread sampledThread = ev.getThread(SAMPLED_THREAD);
      threadName = nameNormalizer.getNormalizedThreadName(sampledThread.getJavaName());
    }

    String threadState = null;
    if (hasField(ev, STATE, SIMPLE_CLASS_NAME)) {
      threadState = ev.getString(STATE);
    }
    JvmStackTraceEvent event = stackTraceToStackFrames(threadName, threadState, ev.getStackTrace());

    stackTraceEventPerThread
        .computeIfAbsent(event.getThreadName(), k -> new ArrayList<>())
        .add(event);
  }

  @Override
  public Stream<Event> summarize() {
    Stream<Event> eventStream =
        stackTraceEventPerThread.entrySet().stream()
            .map(entry -> stackTraceToEvent(entry.getKey(), entry.getValue()))
            .flatMap(Collection::stream);
    return eventStream;
  }

  private List<Event> stackTraceToEvent(String threadName, List<JvmStackTraceEvent> stackTraces) {
    StackFrame stackFrame = stackTraceToStackFrame(stackTraces);
    List<FlameLevel> flameLevels = flattener.flatten(stackFrame);
    return flameLevelToEvent(flameLevels, threadName);
  }

  private List<Event> flameLevelToEvent(List<FlameLevel> flameLevels, String threadName) {
    List<Event> events = new ArrayList<>();
    for (FlameLevel flameLevel : flameLevels) {
      Attributes attr = new Attributes();
      attr.put(THREAD_NAME, threadName);
      attr.put(FLAME_NAME, flameLevel.getName());
      attr.put(FLAME_VALUE, flameLevel.getCount());
      attr.put(FLAME_PARENT_ID, flameLevel.getParentId());
      events.add(new Event(JFR_FLAMELEVEL, attr, timestamp.longValue()));
    }
    return events;
  }

  private StackFrame stackTraceToStackFrame(List<JvmStackTraceEvent> traces) {
    FlamegraphMarshaller out = new FlamegraphMarshaller();
    for (JvmStackTraceEvent trace : traces) {
      Stack<String> stack = new Stack<>();
      trace.getFrames().forEach(f -> stack.push(getFrameName(f)));
      out.processEvent(stack, getValue());
    }
    return out.getStackFrame();
  }

  private Integer getValue() {
    // For CPU stack traces this is always 1 - for other things (mem) it's not
    return 1;
  }

  private String getFrameName(JvmStackTraceEvent.JvmStackFrame frame) {
    StringBuilder entryBuilder = new StringBuilder();
    entryBuilder.append(frame.getDesc());
    entryBuilder.append(":");
    entryBuilder.append(frame.getLine());
    return entryBuilder.toString();
  }

  private static JvmStackTraceEvent stackTraceToStackFrames(
      String name, String state, RecordedStackTrace stackTrace) {
    List<JvmStackTraceEvent.JvmStackFrame> out = new ArrayList<>();

    for (RecordedFrame frame : stackTrace.getFrames()) {
      String desc = MethodSupport.describeMethod(frame.getMethod());
      int line = frame.getLineNumber();
      int bytecodeIndex = frame.getBytecodeIndex();
      out.add(new JvmStackTraceEvent.JvmStackFrame(desc, line, bytecodeIndex));
    }

    return new JvmStackTraceEvent(name, state, out);
  }

  @Override
  public void reset() {
    stackTraceEventPerThread.clear();
    timestamp.set(Long.MAX_VALUE);
  }
}
