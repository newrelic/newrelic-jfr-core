package com.newrelic.jfr;

import com.newrelic.jfr.profiler.EventToEventSummary;
import com.newrelic.jfr.profiler.ProfileSummarizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

class ProfilerRegistryTest {

    @Test
    void testUnknownNamesDropped() {
        var names =
                List.of(
                        "unknown1",
                        ProfileSummarizer.EVENT_NAME,
                        "unknown2",
                        ProfileSummarizer.NATIVE_EVENT_NAME,
                        "unknown3");

        var expected =
                List.of(ProfileSummarizer.EVENT_NAME, ProfileSummarizer.NATIVE_EVENT_NAME);
       ProfilerRegistry registry = ProfilerRegistry.create(names);

        var actual = registry.all().map(EventToEventSummary::getEventName).collect(toList());
        assertEquals(expected, actual);
    }
    
    @Test
    void testGetUnknown() {
        ProfilerRegistry registry = ProfilerRegistry.createDefault();
        assertTrue(registry.get("NOT gonna find me").isEmpty());
    }

    @Test
    void testGetKnown() {
        ProfilerRegistry registry = ProfilerRegistry.createDefault();
        assertTrue(registry.get(ProfileSummarizer.NATIVE_EVENT_NAME).isPresent());
    }
}