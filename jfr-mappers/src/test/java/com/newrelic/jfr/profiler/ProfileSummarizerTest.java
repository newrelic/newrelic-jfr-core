package com.newrelic.jfr.profiler;

import static com.newrelic.jfr.profiler.ProfileSummarizer.FLAME_VALUE;
import static com.newrelic.jfr.profiler.ProfileSummarizer.SAMPLED_THREAD;
import static com.newrelic.jfr.profiler.ProfileSummarizer.STATE;
import static com.newrelic.jfr.profiler.ProfileSummarizer.THREAD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.MethodSupport;
import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.telemetry.events.Event;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ProfileSummarizerTest {

  private final String stackTrace =
      "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":[{\"desc\":\"java.net.PlainSocketImpl.socketAccept(Ljava/net/SocketImpl;)V\",\"line\":\"-1\",\"bytecodeIndex\":\"0\"},{\"desc\":\"java.net.AbstractPlainSocketImpl.accept(Ljava/net/SocketImpl;)V\",\"line\":\"458\",\"bytecodeIndex\":\"7\"},{\"desc\":\"java.net.ServerSocket.implAccept(Ljava/net/Socket;)V\",\"line\":\"551\",\"bytecodeIndex\":\"60\"},{\"desc\":\"java.net.ServerSocket.accept()Ljava/net/Socket;\",\"line\":\"519\",\"bytecodeIndex\":\"48\"},{\"desc\":\"sun.rmi.transport.tcp.TCPTransport$AcceptLoop.executeAcceptLoop()V\",\"line\":\"394\",\"bytecodeIndex\":\"42\"},{\"desc\":\"sun.rmi.transport.tcp.TCPTransport$AcceptLoop.run()V\",\"line\":\"366\",\"bytecodeIndex\":\"1\"},{\"desc\":\"java.lang.Thread.run()V\",\"line\":\"834\",\"bytecodeIndex\":\"11\"}]}";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RecordedEvent mockEvent;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RecordedEvent mockEvent2;

  @Mock private ThreadNameNormalizer nameNormalizer;

  @Mock private RecordedThread mockThread;
  @Mock private RecordedThread mockThread2;
  @Mock private RecordedStackTrace mockStackTrace;
  @Mock private RecordedStackTrace mockStackTrace2;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    when(mockEvent.getThread(SAMPLED_THREAD)).thenReturn(mockThread);
    when(mockThread.getJavaName()).thenReturn("thread-1");
    when(mockEvent.getString(ProfileSummarizer.STATE)).thenReturn("running");
    when(mockEvent.getStackTrace()).thenReturn(mockStackTrace);
    when(mockEvent.getStartTime().toEpochMilli()).thenReturn(1L);

    when(mockEvent2.getThread(SAMPLED_THREAD)).thenReturn(mockThread2);
    when(mockThread2.getJavaName()).thenReturn("thread-2");
    when(mockEvent2.getString(STATE)).thenReturn("running");
    when(mockEvent2.getStackTrace()).thenReturn(mockStackTrace2);
    when(mockEvent2.getStartTime().toEpochMilli()).thenReturn(2L);
  }

  @Test
  public void acceptReturnsTwoThreadsFourEvents() {
    when(nameNormalizer.getNormalizedThreadName(any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class));
    ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample(nameNormalizer);

    try (MockedStatic<MethodSupport> methodSupport = Mockito.mockStatic(MethodSupport.class)) {
      methodSupport.when(() -> MethodSupport.serialize(any())).thenReturn(stackTrace);

      try (MockedStatic<RecordedObjectValidators> recordedObjectValidator =
          Mockito.mockStatic(RecordedObjectValidators.class)) {
        recordedObjectValidator
            .when(() -> RecordedObjectValidators.hasField(any(), any(), any()))
            .thenReturn(true);

        testClass.accept(mockEvent);
        testClass.accept(mockEvent);
        testClass.accept(mockEvent2);
        testClass.accept(mockEvent2);
      }

      Map<String, List<JvmStackTraceEvent>> result = testClass.getStackTraceEventPerThread();

      assertEquals(2, result.size());
      assertEquals(2, result.get("thread-1").size());
      assertEquals(2, result.get("thread-2").size());
    }
  }

  @Test
  public void summarizesCorrectly() {
    when(nameNormalizer.getNormalizedThreadName(any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class));
    ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample(nameNormalizer);

    try (MockedStatic<MethodSupport> methodSupport = Mockito.mockStatic(MethodSupport.class)) {
      methodSupport.when(() -> MethodSupport.serialize(any())).thenReturn(stackTrace);

      try (MockedStatic<RecordedObjectValidators> recordedObjectValidator =
          Mockito.mockStatic(RecordedObjectValidators.class)) {
        recordedObjectValidator
            .when(() -> RecordedObjectValidators.hasField(any(), any(), any()))
            .thenReturn(true);
        testClass.accept(mockEvent);
        testClass.accept(mockEvent);
        testClass.accept(mockEvent2);
        testClass.accept(mockEvent2);
      }

      List<Event> resultEvents = testClass.summarize().collect(Collectors.toList());

      assertEquals(16, resultEvents.size());
      assertEquals(
          8,
          (int)
              resultEvents
                  .stream()
                  .filter(
                      e -> e.getAttributes().asMap().get(THREAD_NAME).toString().equals("thread-1"))
                  .count());
      assertEquals(
          16,
          resultEvents
              .stream()
              .filter(e -> e.getAttributes().asMap().get(THREAD_NAME).toString().equals("thread-1"))
              .mapToInt(e -> (int) e.getAttributes().asMap().get(FLAME_VALUE))
              .sum());
    }
  }

  @Test
  public void summarizesGroupedThreadsCorrectly() {
    when(nameNormalizer.getNormalizedThreadName(any(String.class))).thenReturn("thread-#");
    ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample(nameNormalizer);

    try (MockedStatic<MethodSupport> methodSupport = Mockito.mockStatic(MethodSupport.class)) {
      methodSupport.when(() -> MethodSupport.serialize(any())).thenReturn(stackTrace);

      try (MockedStatic<RecordedObjectValidators> recordedObjectValidator =
          Mockito.mockStatic(RecordedObjectValidators.class)) {
        recordedObjectValidator
            .when(() -> RecordedObjectValidators.hasField(any(), any(), any()))
            .thenReturn(true);
        testClass.accept(mockEvent);
        testClass.accept(mockEvent);
        testClass.accept(mockEvent2);
        testClass.accept(mockEvent2);
      }

      List<Event> resultEvents = testClass.summarize().collect(Collectors.toList());

      assertEquals(8, resultEvents.size());
      assertEquals(
          8,
          (int)
              resultEvents
                  .stream()
                  .filter(
                      e -> e.getAttributes().asMap().get(THREAD_NAME).toString().equals("thread-#"))
                  .count());
      assertEquals(
          32,
          resultEvents
              .stream()
              .filter(e -> e.getAttributes().asMap().get(THREAD_NAME).toString().equals("thread-#"))
              .mapToInt(e -> (int) e.getAttributes().asMap().get(FLAME_VALUE))
              .sum());
    }
  }

  @Test
  public void earliestEventTimestampIsSet() {
    when(nameNormalizer.getNormalizedThreadName(any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class));
    ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample(nameNormalizer);

    try (MockedStatic<MethodSupport> methodSupport = Mockito.mockStatic(MethodSupport.class)) {
      methodSupport.when(() -> MethodSupport.serialize(any())).thenReturn(stackTrace);

      try (MockedStatic<RecordedObjectValidators> recordedObjectValidator =
          Mockito.mockStatic(RecordedObjectValidators.class)) {
        recordedObjectValidator
            .when(() -> RecordedObjectValidators.hasField(any(), any(), any()))
            .thenReturn(true);
        testClass.accept(mockEvent2);
        testClass.accept(mockEvent);
      }

      List<Event> resultEvents = testClass.summarize().collect(Collectors.toList());

      assertEquals(16, resultEvents.size());
      assertEquals(16, resultEvents.stream().filter(e -> e.getTimestamp() == 1L).count());
    }
  }
}
