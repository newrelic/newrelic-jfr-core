package com.newrelic.jfr.profiler;

import static com.newrelic.jfr.RecordedObjectValidators.hasField;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newrelic.jfr.MethodSupport;
import com.newrelic.jfr.profiler.FlamegraphMarshaller.StackFrame;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

public class ProfileSummarizer implements EventToEventSummary {
  public static final String EVENT_NAME = "jdk.ExecutionSample";
  public static final String NATIVE_EVENT_NAME = "jdk.NativeMethodSample";
  public static final String SIMPLE_CLASS_NAME = ProfileSummarizer.class.getSimpleName();
  public static final String THREAD_STATE = "thread.state";
  public static final String STATE = "state";
  public static final String THREAD_NAME = "thread.name";
  public static final String SAMPLED_THREAD = "sampledThread";
  public static final String STACK_TRACE = "stackTrace";
  public static final String JFR_FLAMELEVEL = "JfrFlameLevel";
  public static final String FLAME_NAME = "flamelevel.name";
  public static final String FLAME_VALUE = "flamelevel.value";
  public static final String FLAME_PARENT_ID = "flamelevel.parentId";
  private final FrameFlattener flattener;

  private final String eventName;

  private final Map<String, List<StackTraceEvent>> stackTraceEventPerThread = new HashMap<>();
  private AtomicLong timestamp = new AtomicLong(Long.MAX_VALUE);

  // For tests
  public Map<String, List<StackTraceEvent>> getStackTraceEventPerThread() {
    return stackTraceEventPerThread;
  }

  private ProfileSummarizer(final String eventName, FrameFlattener frameFlattener) {
    this.eventName = eventName;
    this.flattener = frameFlattener;
  }

  public static ProfileSummarizer forExecutionSample() {
    return new ProfileSummarizer(EVENT_NAME, new FrameFlattener());
  }

  public static ProfileSummarizer forNativeMethodSample() {
    return new ProfileSummarizer(NATIVE_EVENT_NAME, new FrameFlattener());
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

    Map<String, String> jfrStackTrace = new HashMap<>();
    RecordedThread sampledThread = null;
    if (hasField(ev, SAMPLED_THREAD, SIMPLE_CLASS_NAME)) {
      sampledThread = ev.getThread(SAMPLED_THREAD);
    }
    jfrStackTrace.put(THREAD_NAME, sampledThread == null ? null : sampledThread.getJavaName());
    if (hasField(ev, STATE, SIMPLE_CLASS_NAME)) {
      jfrStackTrace.put(THREAD_STATE, ev.getString(STATE));
    }
    jfrStackTrace.put(STACK_TRACE, MethodSupport.serialize(ev.getStackTrace()));
    JvmStackTraceEvent event = stackTraceToStackFrames(jfrStackTrace);

    stackTraceEventPerThread.computeIfPresent(
        event.getThreadName(),
        (k, list) -> {
          list.add(event);
          return list;
        });

    stackTraceEventPerThread.computeIfAbsent(
        event.getThreadName(), k -> new ArrayList<>(Arrays.asList(event)));
  }

  @Override
  public Stream<Event> summarize() {
    Map<String, StackFrame> stackFrameByThread =
        stackTraceEventPerThread
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, event -> stackTraceToStackFrame(event.getValue())));

    Map<String, List<FlameLevel>> flameLevelsByThread =
        stackFrameByThread
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, stackframe -> flattener.flatten(stackframe.getValue())));

    List<Event> events =
        flameLevelsByThread
            .entrySet()
            .stream()
            .flatMap(e -> flameLevelToEvent(e.getValue(), e.getKey()).stream())
            .collect(Collectors.toList());
    return events.stream();
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

  private StackFrame stackTraceToStackFrame(List<StackTraceEvent> traces) {
    FlamegraphMarshaller out = new FlamegraphMarshaller();
    for (StackTraceEvent event : traces) {
      if (event instanceof JvmStackTraceEvent) {
        JvmStackTraceEvent trace = (JvmStackTraceEvent) event;
        Stack<String> stack = new Stack<>();
        trace.getFrames().forEach(f -> stack.push(getFrameName(f)));
        out.processEvent(stack, getValue());
      }
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

  private static JvmStackTraceEvent stackTraceToStackFrames(Map<String, String> jfrStackTrace) {
    String name = jfrStackTrace.get(THREAD_NAME);
    String state = jfrStackTrace.get(THREAD_STATE);
    JsonElement jsonTree = JsonParser.parseString(jfrStackTrace.get(STACK_TRACE));
    if (jsonTree.isJsonObject()) {
      JsonObject json = jsonTree.getAsJsonObject();
      JsonArray payload = json.getAsJsonArray("payload");
      List<JvmStackTraceEvent.JvmStackFrame> out = new ArrayList<>();

      for (JsonElement element : payload) {
        if (element.isJsonObject()) {
          JsonObject jFrame = element.getAsJsonObject();
          String desc = jFrame.get("desc").getAsString();
          int line = jFrame.get("line").getAsInt();
          int bytecodeIndex = jFrame.get("bytecodeIndex").getAsInt();

          out.add(new JvmStackTraceEvent.JvmStackFrame(desc, line, bytecodeIndex));
        } else {
          // log error
        }
      }
      return new JvmStackTraceEvent(name, state, out);
    } else {
      return new JvmStackTraceEvent(name, state, Collections.emptyList());
    }
  }

  @Override
  public void reset() {
    stackTraceEventPerThread.clear();
    timestamp.set(Long.MAX_VALUE);
  }
}
