package com.newrelic.jfr.profiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newrelic.jfr.profiler.FlamegraphMarshaller.StackFrame;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

public class ProfileSummarizer implements EventToEventSummary {
  public static final String EVENT_NAME = "jdk.ExecutionSample";
  public static final String NATIVE_EVENT_NAME = "jdk.NativeMethodSample";
  private final FrameFlattener flattener;

  private final String eventName;
  
  // key is thread.name
  private final Map<String, List<StackTraceEvent>> stackTraceEventPerThread = new HashMap<>();

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

    Map<String, String> jfrStackTrace = new HashMap<>();
    RecordedThread sampledThread = ev.getThread("sampledThread");
    jfrStackTrace.put("thread.name", sampledThread == null ? null : sampledThread.getJavaName());
    jfrStackTrace.put("thread.state", ev.getString("state"));
    jfrStackTrace.put("stackTrace", MethodSupport.serialize(ev.getStackTrace()));
    
    //change into JvmStackTraceEvent
    JvmStackTraceEvent event = makeEvent(jfrStackTrace);
    
    //TODO: These lists are being modified in place. Should they be immutable and replaced when event is added?
    //exisiting thread? Add event to list.
    stackTraceEventPerThread.computeIfPresent(event.getThreadName(), (k, list) -> {
      list.add(event);
      return list;
    });
    // event of New Thread? Add new map entry. key and new list <StackTraceEvent>
    stackTraceEventPerThread.computeIfAbsent(event.getThreadName(), k -> new ArrayList<>(Arrays.asList(event)));
  }

  @Override
  public Stream<Event> summarize() {
    //Transform <threadName, List<StackTraceEvent>> into <threadname, StackFrame>
    Map<String, StackFrame> stackFramePerThread = stackTraceEventPerThread.entrySet().stream()
            .collect(Collectors.toMap(
                    e -> e.getKey(),
                    e -> tracesToFlameGraphStackFrame(e.getValue())
            ));
    
    //Transform <threadname, StackFrame> into <thread, List<FrameLevel>>.  A stack frame is many many FrameLevels.
    Map<String, List<FlameLevel>> flameLevelsPerThread = stackFramePerThread.entrySet().stream()
            .collect(Collectors.toMap(
                    e -> e.getKey(),
                    e -> flattener.flatten(e.getValue())
            ));
    
    //transform <thread, List<Framelevel> to List<Event>. Events also have thread.name attribute
    List<Event> events = flameLevelsPerThread.entrySet().stream()
            .flatMap(e -> flameLevelToEvent(e.getValue(), e.getKey()).stream())
            .collect(Collectors.toList());
    return events.stream();
  }

  private List<Event> flameLevelToEvent(List<FlameLevel> flameLevels, String threadName) {
    List<Event> events = new ArrayList<>();
    for(FlameLevel flameLevel: flameLevels) {
      Attributes attr = new Attributes();
      attr.put("thread.name", threadName);
      attr.put("flamelevel.name", flameLevel.getName());
      attr.put("flamelevel.value", flameLevel.getCount());
      //this is redundant, alreaady sending up name which is exaclty the same as id
//      attr.put("id", flameLevel.getId());
      attr.put("flamelevel.parentId", flameLevel.getParentId());
      long timestamp = System.currentTimeMillis();;
      events.add(new Event("JfrFlameLevel", attr, timestamp));
    }
    return events;
  }

  private StackFrame tracesToFlameGraphStackFrame(List<StackTraceEvent> traces) {
    //Setup a new marshaller
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
    String name = jfrStackTrace.get("thread.name").toString();
    String state = jfrStackTrace.get("thread.state").toString();
    JsonElement jsonTree = JsonParser.parseString(jfrStackTrace.get("stackTrace").toString());
    if (jsonTree.isJsonObject()) {
      JsonObject json = jsonTree.getAsJsonObject();
      //      var version = json.get("version").getAsString();
      //      var truncated = json.get("truncated").getAsBoolean();

      //this payload comes from stackTrace
      JsonArray payload = json.getAsJsonArray("payload");
      List<JvmStackTraceEvent.JvmStackFrame> out = new ArrayList<>();

      //every element of payload aka strackTrace
      for (JsonElement element : payload) {
        if (element.isJsonObject()) {
          JsonObject jFrame = element.getAsJsonObject();
          String desc = jFrame.get("desc").getAsString();
          int line = jFrame.get("line").getAsInt();
          int bytecodeIndex = jFrame.get("bytecodeIndex").getAsInt();

          //creates individual frames and adds to list
          out.add(new JvmStackTraceEvent.JvmStackFrame(desc, line, bytecodeIndex));
        } else {
          // log error
        }
      }

      //adds all frames as list belonging to this event.
      return new JvmStackTraceEvent(name, state, out);
    } else {
      // log error
      return new JvmStackTraceEvent(name, state, Collections.emptyList());
    }
  }

  @Override
  public void reset() {
    stackTraceEventPerThread.clear();
  }
}
