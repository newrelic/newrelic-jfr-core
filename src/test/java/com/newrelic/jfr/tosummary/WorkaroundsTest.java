package com.newrelic.jfr.tosummary;

import com.newrelic.jfr.Workarounds;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkaroundsTest {

    @Test
    void testGetThreadNameHappyPath() {
        String threadName = "berger";
        var ev = mock(RecordedEvent.class);
        var thread = mock(RecordedThread.class);
        when(thread.getJavaName()).thenReturn(threadName);
        when(ev.getValue("eventThread")).thenReturn(thread);
        assertEquals(threadName, Workarounds.getThreadName(ev).get());
    }

    @Test
    void testGetThreadNameWrongType() throws Exception {
        var ev = mock(RecordedEvent.class);
        when(ev.getValue("eventThread")).thenReturn(new Object[]{"aa", 21, "bbbbbbb"});
        assertTrue(Workarounds.getThreadName(ev).isEmpty());
    }
}