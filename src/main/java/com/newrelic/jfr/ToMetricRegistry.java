package com.newrelic.jfr;

import com.newrelic.jfr.tometric.*;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class ToMetricRegistry {

    private final Map<String, EventToMetric> mappers;

    private ToMetricRegistry(Map<String, EventToMetric> mappers) {
        this.mappers = mappers;
    }

    public static ToMetricRegistry createDefault() {
        var mappers = Map.of(
                AllocationRequiringGCMapper.EVENT_NAME, new AllocationRequiringGCMapper(),
                ContextSwitchRateMapper.EVENT_NAME, new ContextSwitchRateMapper(),
                CPUThreadLoadMapper.EVENT_NAME, new CPUThreadLoadMapper(),
                GarbageCollectionMapper.EVENT_NAME, new GarbageCollectionMapper(),
                GCHeapSummaryMapper.EVENT_NAME, new GCHeapSummaryMapper(),
                MetaspaceSummaryMapper.EVENT_NAME, new MetaspaceSummaryMapper(),
                OverallCPULoadMapper.EVENT_NAME, new OverallCPULoadMapper(),
                ThreadAllocationStatisticsMapper.EVENT_NAME, new ThreadAllocationStatisticsMapper()
                );
        return new ToMetricRegistry(mappers);
    }

    public static ToMetricRegistry create(Collection<String> eventNames) {
        var all = createDefault();
        var filtered = all.getMappers().entrySet().stream()
                .filter(e -> eventNames.contains(e.getKey()))
                .collect(toMap(n -> n.getKey(), n -> n.getValue()));
        return new ToMetricRegistry(filtered);
    }

    private Map<String, EventToMetric> getMappers() {
        return mappers;
    }

    public EventToMetric get(String eventName) {
        return mappers.get(eventName);
    }
}
