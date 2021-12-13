package com.newrelic.jfr.profiler;

import static com.newrelic.jfr.profiler.ProfileSummarizer.FLAME_VALUE;
import static com.newrelic.jfr.profiler.ProfileSummarizer.SAMPLED_THREAD;
import static com.newrelic.jfr.profiler.ProfileSummarizer.STATE;
import static com.newrelic.jfr.profiler.ProfileSummarizer.THREAD_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.jfr.ThreadNameNormalizer;
import com.newrelic.telemetry.events.Event;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
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

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RecordedEvent mockEvent;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RecordedEvent mockEvent2;

  @Mock private ThreadNameNormalizer nameNormalizer;

  @Mock private RecordedThread mockThread;
  @Mock private RecordedThread mockThread2;
  @Mock private RecordedStackTrace mockStackTrace;

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
    when(mockEvent2.getStackTrace()).thenReturn(mockStackTrace);
    when(mockEvent2.getStartTime().toEpochMilli()).thenReturn(2L);

    List<RecordedFrame> frames =
        Arrays.asList(
            buildFrame(
                "java.net.PlainSocketImpl", "socketAccept", "(Ljava/net/SocketImpl;)V", -1, 0),
            buildFrame(
                "java.net.AbstractPlainSocketImpl", "accept", "(Ljava/net/SocketImpl;)V", 458, 7),
            buildFrame("java.net.ServerSocket", "implAccept", "(Ljava/net/Socket;)V", 551, 60),
            buildFrame("java.net.ServerSocket", "accept", "()Ljava/net/Socket;", 519, 48),
            buildFrame(
                "sun.rmi.transport.tcp.TCPTransport$AcceptLoop",
                "executeAcceptLoop",
                "()V",
                394,
                42),
            buildFrame("sun.rmi.transport.tcp.TCPTransport$AcceptLoop", "run", "()V", 366, 1),
            buildFrame("java.lang.Thread", "run", "()V", 834, 11));
    when(mockStackTrace.getFrames()).thenReturn(frames);
  }

  @Test
  public void acceptReturnsTwoThreadsFourEvents() {
    when(nameNormalizer.getNormalizedThreadName(any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class));
    ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample(nameNormalizer);

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

  @Test
  public void summarizesCorrectly() {
    when(nameNormalizer.getNormalizedThreadName(any(String.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class));
    ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample(nameNormalizer);

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
            resultEvents.stream()
                .filter(
                    e -> e.getAttributes().asMap().get(THREAD_NAME).toString().equals("thread-1"))
                .count());
    assertEquals(
        16,
        resultEvents.stream()
            .filter(e -> e.getAttributes().asMap().get(THREAD_NAME).toString().equals("thread-1"))
            .mapToInt(e -> (int) e.getAttributes().asMap().get(FLAME_VALUE))
            .sum());
  }

  @Test
  public void summarizesGroupedThreadsCorrectly() {
    when(nameNormalizer.getNormalizedThreadName(any(String.class))).thenReturn("thread-#");
    ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample(nameNormalizer);

    try (MockedStatic<RecordedObjectValidators> recordedObjectValidator =
        Mockito.mockStatic(RecordedObjectValidators.class)) {
      recordedObjectValidator
          .when(() -> RecordedObjectValidators.hasField(any(), any(), any()))
          .thenReturn(true);
      testClass.accept(mockEvent);
      testClass.accept(mockEvent);
      testClass.accept(mockEvent2);
      testClass.accept(mockEvent2);

      List<Event> resultEvents = testClass.summarize().collect(Collectors.toList());

      assertEquals(8, resultEvents.size());
      assertEquals(
          8,
          (int)
              resultEvents.stream()
                  .filter(
                      e -> e.getAttributes().asMap().get(THREAD_NAME).toString().equals("thread-#"))
                  .count());
      assertEquals(
          32,
          resultEvents.stream()
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

  private RecordedFrame buildFrame(
      String className, String methodName, String descriptor, int line, int bytecodeIndex) {
    RecordedFrame frame = mock(RecordedFrame.class, Answers.RETURNS_DEEP_STUBS);
    when(frame.getMethod().getType().getName()).thenReturn(className);
    when(frame.getMethod().getName()).thenReturn(methodName);
    when(frame.getMethod().getDescriptor()).thenReturn(descriptor);
    when(frame.getLineNumber()).thenReturn(line);
    when(frame.getBytecodeIndex()).thenReturn(bytecodeIndex);
    return frame;
  }
}
