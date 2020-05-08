package com.newrelic.jfr;

import com.newrelic.jfr.tosummary.*;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class ToSummaryRegistry {

    private final Map<String, EventToSummary> mappers;

    private ToSummaryRegistry(Map<String, EventToSummary> mappers) {
        this.mappers = mappers;
    }

    public static ToSummaryRegistry createDefault() {
        var mappers = Map.of(
                G1GarbageCollectionSummarizer.EVENT_NAME, new G1GarbageCollectionSummarizer(),
                NetworkReadSummarizer.EVENT_NAME, new NetworkReadSummarizer(),
                NetworkWriteSummarizer.EVENT_NAME, new NetworkWriteSummarizer(),
                ObjectAllocationInNewTLABSummarizer.EVENT_NAME, new ObjectAllocationInNewTLABSummarizer(),
                ObjectAllocationOutsideTLABSummarizer.EVENT_NAME, new ObjectAllocationOutsideTLABSummarizer()
                );
        return new ToSummaryRegistry(mappers);
    }

    public static ToSummaryRegistry create(Collection<String> eventNames) {
        var all = createDefault();
        var filtered = all.getMappers().entrySet().stream()
                .filter(e -> eventNames.contains(e.getKey()))
                .collect(toMap(n -> n.getKey(), n -> n.getValue()));
        return new ToSummaryRegistry(filtered);
    }

    private Map<String, EventToSummary> getMappers() {
        return mappers;
    }

    public EventToSummary get(String eventName) {
        return mappers.get(eventName);
    }
}
