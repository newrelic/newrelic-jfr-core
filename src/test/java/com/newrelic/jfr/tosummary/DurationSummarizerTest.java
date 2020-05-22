package com.newrelic.jfr.tosummary;

import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DurationSummarizerTest {

    @Test
    void testAccept() {
        var start1  = System.currentTimeMillis();
        var start2  = start1 + 777;
        var duration1 = Duration.ofSeconds(5);
        var duration2 = Duration.ofSeconds(13);

        var ev1 = mock(RecordedEvent.class);
        var ev2 = mock(RecordedEvent.class);

        when(ev1.getStartTime()).thenReturn(Instant.ofEpochMilli(start1));
        when(ev1.getDuration()).thenReturn(duration1);
        when(ev2.getStartTime()).thenReturn(Instant.ofEpochMilli(start2));
        when(ev2.getDuration()).thenReturn(duration2);

        DurationSummarizer testClass = new DurationSummarizer(start1);
        testClass.accept(ev1);
        testClass.accept(ev2);

        assertEquals(18000, testClass.getDurationMillis());
        assertEquals(start1, testClass.getStartTimeMs());
        assertEquals(start2 + 13000, testClass.getEndTimeMs());
        assertEquals(13000, testClass.getMaxDurationMillis());
        assertEquals(5000, testClass.getMinDurationMillis());
    }

    @Test
    void testDefaultState() {
        var start  = System.currentTimeMillis();

        DurationSummarizer testClass = new DurationSummarizer(start);

        assertEquals(0, testClass.getDurationMillis());
        assertEquals(start, testClass.getStartTimeMs());
        assertEquals(start, testClass.getEndTimeMs());
        assertEquals(Duration.ofNanos(Long.MIN_VALUE).toMillis(), testClass.getMaxDurationMillis());
        assertEquals(Duration.ofNanos(Long.MAX_VALUE).toMillis(), testClass.getMinDurationMillis());
    }

    @Test
    void testReset() {
        var start1  = System.currentTimeMillis();
        var start2  = start1 + 1234;
        var duration1 = Duration.ofSeconds(11);
        var duration2 = Duration.ofSeconds(23);

        var ev1 = mock(RecordedEvent.class);
        var ev2 = mock(RecordedEvent.class);

        when(ev1.getStartTime()).thenReturn(Instant.ofEpochMilli(start1));
        when(ev1.getDuration()).thenReturn(duration1);
        when(ev2.getStartTime()).thenReturn(Instant.ofEpochMilli(start2));
        when(ev2.getDuration()).thenReturn(duration2);

        DurationSummarizer testClass = new DurationSummarizer(start1, () -> start2);
        testClass.accept(ev1);
        testClass.reset();
        testClass.accept(ev2);

        assertEquals(duration2.toMillis(), testClass.getDurationMillis());
        assertEquals(start2, testClass.getStartTimeMs());
        assertEquals(start2 + duration2.toMillis(), testClass.getEndTimeMs());
        assertEquals(23000, testClass.getMaxDurationMillis());
        assertEquals(23000, testClass.getMinDurationMillis());
    }

    @Test
    void testNamedDurationField() {
        var start  = System.currentTimeMillis();
        var duration = Duration.ofSeconds(1101);

        var ev = mock(RecordedEvent.class);

        when(ev.getStartTime()).thenReturn(Instant.ofEpochMilli(start));
        when(ev.getDuration()).thenReturn(duration);

        DurationSummarizer testClass = new DurationSummarizer(start, DurationSummarizer.DEFAULT_CLOCK, "myDuration");
        testClass.accept(ev);
        testClass.reset();
        testClass.accept(ev);
    }

}