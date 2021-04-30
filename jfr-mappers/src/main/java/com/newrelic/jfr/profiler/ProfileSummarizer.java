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

  private final String eventName;

//  private final List<Map<String, String>> raw = new ArrayList<>();
  
  // key is thread.name
  private final Map<String, List<StackTraceEvent>> stackTraceEventPerThread = new HashMap<>();

  private ProfileSummarizer(final String eventName) {
    this.eventName = eventName;
  }

  public static ProfileSummarizer forExecutionSample() {
    return new ProfileSummarizer(EVENT_NAME);
  }

  public static ProfileSummarizer forNativeMethodSample() {
    return new ProfileSummarizer(NATIVE_EVENT_NAME);
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

    long timestamp = ev.getStartTime().toEpochMilli();
    Map<String, String> jfrStackTrace = new HashMap<>();
    RecordedThread sampledThread = ev.getThread("sampledThread");
    jfrStackTrace.put("thread.name", sampledThread == null ? null : sampledThread.getJavaName());
    jfrStackTrace.put("thread.state", ev.getString("state"));
    jfrStackTrace.put("stackTrace", MethodSupport.serialize(ev.getStackTrace()));
    
    //change into JvmStackTraceEvent
    JvmStackTraceEvent event = makeEvent(jfrStackTrace);
    
    //exisiting thread? Add to list. New Thread? Create map key and new list
    stackTraceEventPerThread.computeIfPresent(event.getThreadName(), (k, list) -> {
      list.add(event);
      return list;
    });
    stackTraceEventPerThread.computeIfAbsent(event.getThreadName(), k -> new ArrayList<>(Arrays.asList(event)));
    
  }

  @Override
  public Stream<Event> summarize() {
    //old way, not by thread result. probably delete this
//    List<StackFrame> stackFrames = stackTraceEventPerThread.values().stream().map(this::tracesToStackFrame).collect(Collectors.toList());

    //Transform <threadName, List<StackTrace>> into <threadname, StackFrame>
    Map<String, StackFrame> stackFramePerThread = stackTraceEventPerThread.entrySet().stream()
            .collect(Collectors.toMap(
                    e -> e.getKey(),
                    e -> tracesToStackFrame(e.getValue())
            ));
    
    //Transform <threadname, StackFrame> into List<Event>. 
    List<Event> events = stackFramePerThread.entrySet().stream()
            .map(e -> stackFrameToEvent(e))
            .collect(Collectors.toList());
    
    return events.stream();
  }

  private Event stackFrameToEvent(Map.Entry<String, StackFrame> e) {
    Attributes attr = new Attributes();
    attr.put("thread.name", e.getKey());
    attr.put("aggregate.stackTrace", e.getValue().toString());
    long timestamp = System.currentTimeMillis();;
    return new Event("JfrAggMethodSample", attr, timestamp);
  }

  private StackFrame tracesToStackFrame(List<StackTraceEvent> traces) {
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
  public void reset() {}
}
