package com.newrelic.jfr.profiler;

import com.newrelic.telemetry.events.Event;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;

public class ProfileSummarizer implements EventToEventSummary {
  public static final String EVENT_NAME = "jdk.ExecutionSample";
  public static final String NATIVE_EVENT_NAME = "jdk.NativeMethodSample";

  private final String eventName;

  private final List<Map<String, String>> raw = new ArrayList<>();

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
    Map<String, String> attr = new HashMap<>();
    RecordedThread sampledThread = ev.getThread("sampledThread");
    attr.put("thread.name", sampledThread == null ? null : sampledThread.getJavaName());
    attr.put("thread.state", ev.getString("state"));
    attr.put("stackTrace", MethodSupport.serialize(ev.getStackTrace()));

    raw.add(attr);
  }

  @Override
  public Stream<Event> summarize() {

    return null;
  }

  @Override
  public void reset() {}
}
