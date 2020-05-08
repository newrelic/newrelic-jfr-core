package com.newrelic.jfr.stacktrace;

import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StackTraceBlobTest {

    private RecordingFile loadFile(String fName) throws IOException, URISyntaxException {
        URL url = StackTraceBlobTest.class.getClassLoader().getResource(fName);
        return new RecordingFile(Paths.get(url.toURI()));
    }

    @Test
    @Disabled
    public void simple_encode() throws Exception {
        var count = 0;
        var max = 0;
        var histo = new int[32];
        try (var recordingFile = loadFile("need-file.jfr")) {
            LOOP: while (recordingFile.hasMoreEvents()) {
                var event = recordingFile.readEvent();
                if (event != null) {
                    var eventType = event.getEventType().getName();
                    if (eventType.equals("jdk.ExecutionSample") || eventType.equals("jdk.NativeMethodSample")) {
                        var trace = event.getStackTrace();
                        var b64 = StackTraceBlob.encodeB64(trace);
                        assertTrue(b64.startsWith("eyJ0eXBlIjoic3RhY2t0cmFjZSIsImxhbmd1YWdlIjoiamF2Y"));
//                        assertEquals("XXeyJ0eXBlIjoic3", StackTraceBlob.encodeB64(trace));
                        count = count + 1;
                        if (max < b64.length()) {
                            max = b64.length();
                        }
                        var bucket = b64.length() / 1000;
                        histo[bucket] = histo[bucket] + 1;
                    }
                }
            }
        }
        assertEquals(218684, count);
        assertEquals(16860, max);
        assertEquals(159, histo[0]);
        assertEquals(65435, histo[1]);
        assertEquals(116643, histo[2]);
        assertEquals(24899, histo[3]); // 94.72% fit under the 4k limit
        assertEquals(6303, histo[4]);
        assertEquals(3994, histo[5]);
        assertEquals(766, histo[6]);
        assertEquals(195, histo[7]); // 99.87% fit in 2 * 4k
    }
}
