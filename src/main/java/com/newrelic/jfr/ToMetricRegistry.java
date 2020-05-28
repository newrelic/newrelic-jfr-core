package com.newrelic.jfr;

import com.newrelic.jfr.tometric.AllocationRequiringGCMapper;
import com.newrelic.jfr.tometric.CPUThreadLoadMapper;
import com.newrelic.jfr.tometric.ContextSwitchRateMapper;
import com.newrelic.jfr.tometric.EventToMetric;
import com.newrelic.jfr.tometric.GCHeapSummaryMapper;
import com.newrelic.jfr.tometric.GarbageCollectionMapper;
import com.newrelic.jfr.tometric.MetaspaceSummaryMapper;
import com.newrelic.jfr.tometric.OverallCPULoadMapper;
import com.newrelic.jfr.tometric.ThreadAllocationStatisticsMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

public class ToMetricRegistry {

    private final static List<EventToMetric> ALL_MAPPERS =
            List.of(
                    new AllocationRequiringGCMapper(),
                    new ContextSwitchRateMapper(),
                    new CPUThreadLoadMapper(),
                    new GarbageCollectionMapper(),
                    new GCHeapSummaryMapper(),
                    new MetaspaceSummaryMapper(),
                    new OverallCPULoadMapper(),
                    new ThreadAllocationStatisticsMapper()
            );
    private final List<EventToMetric> mappers;


    private ToMetricRegistry(List<EventToMetric> mappers) {
        this.mappers = new ArrayList<>(mappers);
    }

    public static ToMetricRegistry createDefault() {
        return create(allEventNames());
    }

    public static ToMetricRegistry create(Collection<String> eventNames) {
        var filtered = ALL_MAPPERS.stream()
                        .filter(mapper -> eventNames.contains(mapper.getEventName()))
                        .collect(toList());
        return new ToMetricRegistry(filtered);
    }

    private static List<String> allEventNames() {
        return ALL_MAPPERS.stream()
                .map(EventToMetric::getEventName)
                .collect(toUnmodifiableList());
    }

    /**
     * @return a stream of all EventToMetric entries in this registry.
     */
    public Stream<EventToMetric> all() {
        return mappers.stream();
    }

    /**
     * Returns an Optional<EventToMetric> containing the mapper with the given
     * JFR event name.  If the event is not known to this registry, the
     * returned Optional will be empty.
     * @param eventName - the JFR name of the event to find
     * @return - an optional EventToMetric.
     */
    public Optional<EventToMetric> get(String eventName) {
        return mappers.stream().filter(toMetric -> toMetric.getEventName().equals(eventName)).findFirst();
    }
}
