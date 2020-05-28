package com.newrelic.jfr;

import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.jfr.tosummary.G1GarbageCollectionSummarizer;
import com.newrelic.jfr.tosummary.NetworkReadSummarizer;
import com.newrelic.jfr.tosummary.NetworkWriteSummarizer;
import com.newrelic.jfr.tosummary.ObjectAllocationInNewTLABSummarizer;
import com.newrelic.jfr.tosummary.ObjectAllocationOutsideTLABSummarizer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

public class ToSummaryRegistry {

    private final static List<EventToSummary> ALL_MAPPERS = List.of(
            new G1GarbageCollectionSummarizer(),
            new NetworkReadSummarizer(),
            new NetworkWriteSummarizer(),
            new ObjectAllocationInNewTLABSummarizer(),
            new ObjectAllocationOutsideTLABSummarizer()
    );

    private final List<EventToSummary> mappers;

    private ToSummaryRegistry(List<EventToSummary> mappers) {
        this.mappers = mappers;
    }

    public static ToSummaryRegistry createDefault() {
        return new ToSummaryRegistry(ALL_MAPPERS);
    }

    public static ToSummaryRegistry create(Collection<String> eventNames) {
        var filtered =
                ALL_MAPPERS
                        .stream()
                        .filter(mapper -> eventNames.contains(mapper.getEventName()))
                        .collect(toList());
        return new ToSummaryRegistry(filtered);
    }


    private static List<String> allEventNames() {
        return ALL_MAPPERS.stream().map(EventToSummary::getEventName).collect(toUnmodifiableList());
    }

    public Stream<EventToSummary> all() {
        return mappers.stream();
    }

    public Optional<EventToSummary> get(String eventName) {
        return mappers.stream()
        .filter(m -> m.getEventName().equals(eventName))
        .findFirst();
    }
}
