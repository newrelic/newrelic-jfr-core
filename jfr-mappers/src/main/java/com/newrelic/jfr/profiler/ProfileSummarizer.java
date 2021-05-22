package com.newrelic.jfr.profiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newrelic.jfr.profiler.FlamegraphMarshaller.StackFrame;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  // key is thread.name
  private final Map<String, List<StackTraceEvent>> stackTraceEventPerThread = new HashMap<>();
  private AtomicLong timestamp = new AtomicLong(Long.MAX_VALUE);

  //For tests
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
    //event with earliest timestamp in this batch, sets timestamp for all batch events
    timestamp.updateAndGet(current -> Math.min(current, ev.getStartTime().toEpochMilli()));
    
    Map<String, String> jfrStackTrace = new HashMap<>();
    RecordedThread sampledThread = ev.getThread(SAMPLED_THREAD);
    jfrStackTrace.put(THREAD_NAME, sampledThread == null ? null : sampledThread.getJavaName());
    jfrStackTrace.put(THREAD_STATE, ev.getString(STATE));
    jfrStackTrace.put(STACK_TRACE, MethodSupport.serialize(ev.getStackTrace()));
    JvmStackTraceEvent event = makeEvent(jfrStackTrace);
    
    //event of an existing thread
    stackTraceEventPerThread.computeIfPresent(
        event.getThreadName(),
        (k, list) -> {
          list.add(event);
          return list;
        });
    // First event of a new thread.
    stackTraceEventPerThread.computeIfAbsent(
        event.getThreadName(), k -> new ArrayList<>(Arrays.asList(event)));
  }

  @Override
  public Stream<Event> summarize() {
    // Transform <threadName, List<StackTraceEvent>> into <threadname, StackFrame>
    Map<String, StackFrame> stackFramePerThread =
        stackTraceEventPerThread
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(e -> e.getKey(), e -> tracesToFlameGraphStackFrame(e.getValue())));

    // Transform <threadname, StackFrame> into <thread, List<FrameLevel>>.  A stack frame is many
    // many FrameLevels.
    Map<String, List<FlameLevel>> flameLevelsPerThread =
        stackFramePerThread
            .entrySet()
            .stream()
            .collect(Collectors.toMap(e -> e.getKey(), e -> flattener.flatten(e.getValue())));

    // transform <thread, List<Framelevel> to List<Event>. Events also have thread.name attribute
    List<Event> events =
        flameLevelsPerThread
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
      ;
      events.add(new Event(JFR_FLAMELEVEL, attr, timestamp.longValue()));
    }
    return events;
  }

  private StackFrame tracesToFlameGraphStackFrame(List<StackTraceEvent> traces) {
    // Setup a new marshaller
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

  private static JvmStackTraceEvent makeEvent(Map<String, String> jfrStackTrace) {
    String name = jfrStackTrace.get(THREAD_NAME).toString();
    String state = jfrStackTrace.get(THREAD_STATE).toString();
    JsonElement jsonTree = JsonParser.parseString(jfrStackTrace.get(STACK_TRACE).toString());
    if (jsonTree.isJsonObject()) {
      JsonObject json = jsonTree.getAsJsonObject();
      //      var version = json.get("version").getAsString();
      //      var truncated = json.get("truncated").getAsBoolean();

      // this payload comes from stackTrace
      JsonArray payload = json.getAsJsonArray("payload");
      List<JvmStackTraceEvent.JvmStackFrame> out = new ArrayList<>();

      // every element of payload aka strackTrace
      for (JsonElement element : payload) {
        if (element.isJsonObject()) {
          JsonObject jFrame = element.getAsJsonObject();
          String desc = jFrame.get("desc").getAsString();
          int line = jFrame.get("line").getAsInt();
          int bytecodeIndex = jFrame.get("bytecodeIndex").getAsInt();

          // creates individual frames and adds to list
          out.add(new JvmStackTraceEvent.JvmStackFrame(desc, line, bytecodeIndex));
        } else {
          // log error
        }
      }

      // adds all frames as list belonging to this event.
      return new JvmStackTraceEvent(name, state, out);
    } else {
      // log error
      return new JvmStackTraceEvent(name, state, Collections.emptyList());
    }
  }

  @Override
  public void reset() {
    stackTraceEventPerThread.clear();
    timestamp.set(0);
  }
}
