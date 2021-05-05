package com.newrelic.jfr.toevent;

import static com.newrelic.jfr.toevent.JITCompilationMapper.DESC;
import static com.newrelic.jfr.toevent.JITCompilationMapper.DURATION;
import static com.newrelic.jfr.toevent.JITCompilationMapper.EVENT_NAME;
import static com.newrelic.jfr.toevent.JITCompilationMapper.EVENT_THREAD;
import static com.newrelic.jfr.toevent.JITCompilationMapper.JFR_COMPILATION;
import static com.newrelic.jfr.toevent.JITCompilationMapper.METHOD;
import static com.newrelic.jfr.toevent.JITCompilationMapper.SUCCEEDED;
import static com.newrelic.jfr.toevent.JITCompilationMapper.THREAD_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class JITCompilationMapperTest {
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;
  private static MockedStatic<Workarounds> workaroundsMockedStatic;
  private static final String MISSING = "[missing]";

  @BeforeAll
  static void init() {
    workaroundsMockedStatic = Mockito.mockStatic(Workarounds.class);
    workaroundsMockedStatic
        .when(() -> Workarounds.getSucceeded(any(RecordedEvent.class)))
        .thenReturn(true);

    recordedObjectValidatorsMockedStatic = Mockito.mockStatic(RecordedObjectValidators.class);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.hasField(
                    any(RecordedObject.class), anyString(), anyString()))
        .thenReturn(true);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.isRecordedObjectNull(
                    any(RecordedObject.class), anyString()))
        .thenReturn(false);
  }

  @AfterAll
  static void teardown() {
    workaroundsMockedStatic.close();
    recordedObjectValidatorsMockedStatic.close();
  }

  @Test
  void testApply() {
    var startTime = Instant.now();
    var threadName = "wonder";
    var duration = Duration.ofSeconds(14);
    var expectedAttrs =
        new Attributes()
            .put(THREAD_NAME, threadName)
            .put(DURATION, duration.toMillis())
            .put(DESC, MISSING)
            .put(SUCCEEDED, true);
    var expectedEvent = new Event(JFR_COMPILATION, expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var eventThread = mock(RecordedThread.class);
    var eventType = mock(EventType.class);

    when(event.getStartTime()).thenReturn(startTime);
    when(event.getDuration()).thenReturn(duration);
    when(event.getThread(EVENT_THREAD)).thenReturn(eventThread);
    when(event.getValue(METHOD)).thenReturn(null);
    when(event.hasField(SUCCEEDED)).thenReturn(true);
    when(event.getBoolean(SUCCEEDED)).thenReturn(true);
    when(event.getEventType()).thenReturn(eventType);

    when(eventThread.getJavaName()).thenReturn(threadName);
    when(eventType.getName()).thenReturn(EVENT_NAME);

    var mapper = new JITCompilationMapper();
    assertTrue(mapper.test(event));

    var result = mapper.apply(event);
    assertEquals(expected, result);
  }

  @Test
  void false_positive_name() throws Exception {
    // "jdk.CompilationFailure";

    var startTime = Instant.now();
    var threadName = "wonder";
    var duration = Duration.ofSeconds(14);
    var expectedAttrs =
        new Attributes()
            .put(THREAD_NAME, threadName)
            .put(DURATION, duration.toMillis())
            .put(DESC, MISSING)
            .put(SUCCEEDED, true);
    var expectedEvent = new Event(JFR_COMPILATION, expectedAttrs, startTime.toEpochMilli());
    var expected = List.of(expectedEvent);

    var event = mock(RecordedEvent.class);
    var eventThread = mock(RecordedThread.class);
    var eventType = mock(EventType.class);

    when(event.getStartTime()).thenReturn(startTime);
    when(event.getDuration()).thenReturn(duration);
    when(event.getThread(EVENT_THREAD)).thenReturn(eventThread);
    when(event.getValue(METHOD)).thenReturn(null);
    when(event.hasField(SUCCEEDED)).thenReturn(true);
    when(event.getBoolean(SUCCEEDED)).thenReturn(true);
    when(event.getEventType()).thenReturn(eventType);

    when(eventThread.getJavaName()).thenReturn(threadName);
    when(eventType.getName()).thenReturn("jdk.CompilationFailure");

    var mapper = new JITCompilationMapper();
    assertFalse(mapper.test(event));
  }

  @Test
  void read_real_event() throws Exception {
    final Path dumpFile = Path.of("src", "test", "resources", "startup3.jfr");

    var mapper = new JITCompilationMapper();
    var seenEvent = false;
    try (final var recordingFile = new RecordingFile(dumpFile)) {
      while (recordingFile.hasMoreEvents()) {
        var event = recordingFile.readEvent();

        if (event != null) {
          if (event.getEventType().getName().equals(EVENT_NAME)) {
            var le = mapper.apply(event);
            assertEquals(1, le.size());
            assertEquals(109L, le.get(0).getAttributes().asMap().get(DURATION));
            assertEquals(
                "java.lang.invoke.LambdaForm$Name.replaceNames([Ljava/lang/invoke/LambdaForm$Name;[Ljava/lang/invoke/LambdaForm$Name;II)Ljava/lang/invoke/LambdaForm$Name;",
                le.get(0).getAttributes().asMap().get(DESC));
            seenEvent = true;
            break;
          }
        }
      }
    }
    assertTrue(seenEvent);
  }
}
