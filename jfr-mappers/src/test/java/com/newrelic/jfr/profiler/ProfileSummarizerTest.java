package com.newrelic.jfr.profiler;

import com.newrelic.telemetry.events.Event;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ProfileSummarizerTest {

    private final String stackTrace = "{\"type\":\"stacktrace\",\"language\":\"java\",\"version\":1,\"truncated\":false,\"payload\":[{\"desc\":\"java.net.PlainSocketImpl.socketAccept(Ljava/net/SocketImpl;)V\",\"line\":\"-1\",\"bytecodeIndex\":\"0\"},{\"desc\":\"java.net.AbstractPlainSocketImpl.accept(Ljava/net/SocketImpl;)V\",\"line\":\"458\",\"bytecodeIndex\":\"7\"},{\"desc\":\"java.net.ServerSocket.implAccept(Ljava/net/Socket;)V\",\"line\":\"551\",\"bytecodeIndex\":\"60\"},{\"desc\":\"java.net.ServerSocket.accept()Ljava/net/Socket;\",\"line\":\"519\",\"bytecodeIndex\":\"48\"},{\"desc\":\"sun.rmi.transport.tcp.TCPTransport$AcceptLoop.executeAcceptLoop()V\",\"line\":\"394\",\"bytecodeIndex\":\"42\"},{\"desc\":\"sun.rmi.transport.tcp.TCPTransport$AcceptLoop.run()V\",\"line\":\"366\",\"bytecodeIndex\":\"1\"},{\"desc\":\"java.lang.Thread.run()V\",\"line\":\"834\",\"bytecodeIndex\":\"11\"}]}";
    
    @Mock
    private RecordedEvent mockEvent;
    @Mock
    private RecordedEvent mockEvent2;
    @Mock
    private RecordedThread mockThread;
    @Mock
    private RecordedThread mockThread2;
    @Mock
    private RecordedStackTrace mockStackTrace;
    @Mock
    private RecordedStackTrace mockStackTrace2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mockEvent.getThread("sampledThread")).thenReturn(mockThread);
        when(mockThread.getJavaName()).thenReturn("thread-1");
        when(mockEvent.getString("state")).thenReturn("running");
        when(mockEvent.getStackTrace()).thenReturn(mockStackTrace);

        when(mockEvent2.getThread("sampledThread")).thenReturn(mockThread2);
        when(mockThread2.getJavaName()).thenReturn("thread-2");
        when(mockEvent2.getString("state")).thenReturn("running");
        when(mockEvent2.getStackTrace()).thenReturn(mockStackTrace2);
    }

    @Test
    public void acceptReturnsTwoThreadsFourEvents() {
        
        ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample();
        
        try (MockedStatic<MethodSupport> methodSupport = Mockito.mockStatic(MethodSupport.class)) {
            methodSupport.when(() -> MethodSupport.serialize(any()))
                    .thenReturn(stackTrace);
            
            testClass.accept(mockEvent);
            testClass.accept(mockEvent);
            testClass.accept(mockEvent2);
            testClass.accept(mockEvent2);

            Map<String, List<StackTraceEvent>> result = testClass.getStackTraceEventPerThread();
            assertEquals(result.size(), 2);
            assertEquals(result.get("thread-1").size(), 2);
            assertEquals(result.get("thread-2").size(), 2);

        }
    }
    
    @Test
    public void summarizesCorrectly() {
        ProfileSummarizer testClass = ProfileSummarizer.forExecutionSample();

        try (MockedStatic<MethodSupport> methodSupport = Mockito.mockStatic(MethodSupport.class)) {
            methodSupport.when(() -> MethodSupport.serialize(any()))
                    .thenReturn(stackTrace);

            testClass.accept(mockEvent);
            testClass.accept(mockEvent);
            testClass.accept(mockEvent2);
            testClass.accept(mockEvent2);

            List<Event> resultEvents = testClass.summarize().collect(Collectors.toList());
            
            assertEquals(16, resultEvents.size());
            
            assertEquals(8, resultEvents.stream().filter(e -> {
                var threadname = (String) e.getAttributes().asMap().get("thread.name");
                return threadname.equals("thread-1");
            }).collect(Collectors.toList()).size());
            
            assertEquals(16, resultEvents.stream().filter(e -> {
                var threadname = (String) e.getAttributes().asMap().get("thread.name");
                return threadname.equals("thread-1");
            }).mapToInt(e -> {
                return (int) e.getAttributes().asMap().get("flamelevel.value");
            }).sum());

        }
        /* java.net.PlainSocketImpl.socketAccept(Ljava/net/SocketImpl;)V
         * java.net.AbstractPlainSocketImpl.accept(Ljava/net/SocketImpl;)V
         * java.net.ServerSocket.implAccept(Ljava/net/Socket;)V
         * java.net.ServerSocket.accept()Ljava/net/Socket;
         * sun.rmi.transport.tcp.TCPTransport$AcceptLoop.executeAcceptLoop()V
         * sun.rmi.transport.tcp.TCPTransport$AcceptLoop.run()V
         */
    }
}

