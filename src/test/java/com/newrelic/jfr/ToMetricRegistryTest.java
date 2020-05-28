package com.newrelic.jfr;

import com.newrelic.jfr.tometric.CPUThreadLoadMapper;
import com.newrelic.jfr.tometric.EventToMetric;
import com.newrelic.jfr.tometric.GCHeapSummaryMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToMetricRegistryTest {

    @Test
    void testUnknownNamesDropped() {
        var names = List.of("unknown1", CPUThreadLoadMapper.EVENT_NAME, "unknown2",
                GCHeapSummaryMapper.EVENT_NAME, "unknown3");

        var expected = List.of(CPUThreadLoadMapper.EVENT_NAME, GCHeapSummaryMapper.EVENT_NAME);
        ToMetricRegistry registry = ToMetricRegistry.create(names);

        var actual = registry.all().map(EventToMetric::getEventName).collect(toList());
        assertEquals(expected, actual);
    }

    @Test
    void testGetUnknown() {
        ToMetricRegistry registry = ToMetricRegistry.createDefault();
        assertTrue(registry.get("nope").isEmpty());
    }

    @Test
    void testGetKnown() {
        ToMetricRegistry registry = ToMetricRegistry.createDefault();
        assertTrue(registry.get(CPUThreadLoadMapper.EVENT_NAME).isPresent());
    }
}