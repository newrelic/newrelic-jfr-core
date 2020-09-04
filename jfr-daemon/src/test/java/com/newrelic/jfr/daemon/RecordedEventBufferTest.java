package com.newrelic.jfr.daemon;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordedEventBufferTest {

    @Test
    public void testSimpleSinlgePass() throws Exception {

        var queue = new ArrayBlockingQueue<RecordedEvent>(10);
        var dumpPath = new File("/tmp/file.jfr").toPath();

        var e1 = makeEvent(12345);
        var e2 = makeEvent(12346);
        var recordingFile = mock(RecordingFile.class);

        when(recordingFile.hasMoreEvents()).thenReturn(true, true, false);
        when(recordingFile.readEvent()).thenReturn(e1, e2);

        var testClass = new RecordedEventBuffer(queue);
        testClass.bufferEvents(dumpPath, recordingFile);
        assertEquals(2, queue.size());
        var result = testClass.drainToStream().collect(Collectors.toList());
        assertEquals(List.of(e1, e2), result);
        assertEquals(0, queue.size());
    }

    @Test
    public void testMultiplePassesWithContext() throws Exception {

        var queue = new ArrayBlockingQueue<RecordedEvent>(10);
        var dumpPath = new File("/tmp/file.jfr").toPath();

        var e11 = makeEvent(12345);
        var e12 = makeEvent(12346);
        var e21 = makeEvent(12345);  // before the end of the first file
        var e22 = makeEvent(12347);
        var recordingFile1 = mock(RecordingFile.class);
        var recordingFile2 = mock(RecordingFile.class);

        when(recordingFile1.hasMoreEvents()).thenReturn(true, true, false);
        when(recordingFile1.readEvent()).thenReturn(e11, e12);

        when(recordingFile2.hasMoreEvents()).thenReturn(true, true, false);
        when(recordingFile2.readEvent()).thenReturn(e21, e22);

        var testClass = new RecordedEventBuffer(queue);
        testClass.bufferEvents(dumpPath, recordingFile1);
        testClass.bufferEvents(dumpPath, recordingFile2);
        assertEquals(3, queue.size());
        var result = testClass.drainToStream().collect(Collectors.toList());
        assertEquals(List.of(e11, e12, e22), result);
        assertEquals(0, queue.size());
    }


    private RecordedEvent makeEvent(long ms) {
        return makeEvent(Instant.ofEpochMilli(ms));
    }

    private RecordedEvent makeEvent(Instant startTime) {
        var result = mock(RecordedEvent.class);
        when(result.getStartTime()).thenReturn(startTime);
        return result;
    }

}