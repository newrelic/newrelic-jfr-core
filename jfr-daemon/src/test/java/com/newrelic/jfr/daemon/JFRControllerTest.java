package com.newrelic.jfr.daemon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.openmbean.OpenDataException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JFRControllerTest {

    @Mock
    JFRJMXRecorder recorder;
    @Mock
    DumpFileProcessor uploader;

    @Test
    void testRunHappyPath() throws Exception {
        var shutdownWasCalled = new AtomicBoolean(false);
        var filePath = Path.of("/foo", "path", "here");
        var config = DaemonConfig.builder()
                .apiKey("abc123sekretcafe")
                .harvestInterval(Duration.ofMillis(1))
                .build();

        when(recorder.recordToFile()).thenReturn(filePath);

        var executorService = Executors.newSingleThreadScheduledExecutor();
        var testClass = new JFRController(uploader, config, executorService) {
            @Override
            JFRJMXRecorder connect() throws IOException {
                return recorder;
            }
        };

        doAnswer(x -> {
            testClass.shutdown();
            shutdownWasCalled.set(true);
            return null;
        }).when(uploader).handleFile(filePath);

        testClass.setup();
        testClass.runUntilShutdown();

        verify(recorder).startRecordingWithBackOff();
        assertTrue(shutdownWasCalled.get());
        assertTrue(executorService.isShutdown());
    }

    @Test
    void testRecordingFailsAndWeRestart() throws Exception {
        var shutdownWasCalled = new AtomicBoolean(false);
        var filePath = Path.of("/foo", "path", "here");
        var config = DaemonConfig.builder()
                .apiKey("abc123sekretcafe")
                .harvestInterval(Duration.ofMillis(1))
                .build();

        when(recorder.recordToFile()).thenThrow(new IOException("it hurts!")).thenReturn(filePath);

        var executorService = Executors.newSingleThreadScheduledExecutor();
        var testClass = new JFRController(uploader, config, executorService) {
            @Override
            JFRJMXRecorder connect() throws IOException {
                return recorder;
            }
        };

        doAnswer(x -> {
            testClass.shutdown();
            shutdownWasCalled.set(true);
            return null;
        }).when(uploader).handleFile(filePath);

        testClass.setup();
        testClass.runUntilShutdown();

        verify(recorder, times(2)).startRecordingWithBackOff();
        assertTrue(shutdownWasCalled.get());
        assertTrue(executorService.isShutdown());
    }

    @Test
    void testRestartFailureShutsDown() throws Exception {
        var shutdownWasCalled = new AtomicBoolean(false);
        var filePath = Path.of("/foo", "path", "here");
        var config = DaemonConfig.builder()
                .apiKey("abc123sekretcafe")
                .harvestInterval(Duration.ofMillis(1))
                .build();

        var recorder2 = mock(JFRJMXRecorder.class);
        var recorders = new ArrayList<>(List.of(recorder, recorder2));

        when(recorder.recordToFile()).thenThrow(new IOException("it hurts!")).thenReturn(filePath);
        doThrow(new OpenDataException("not going to happen today")).when(recorder2).startRecordingWithBackOff();

        var executorService = Executors.newSingleThreadScheduledExecutor();
        var testClass = new JFRController(uploader, config, executorService) {
            @Override
            JFRJMXRecorder connect() {
                return recorders.remove(0);
            }
        };

        testClass.setup();
        testClass.runUntilShutdown();

        verify(recorder, times(1)).startRecordingWithBackOff();
        verify(recorder2).startRecordingWithBackOff();
        assertTrue(executorService.isShutdown());
    }

}