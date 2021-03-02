/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr;

import static java.util.stream.Collectors.toList;

import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.jfr.tosummary.G1GarbageCollectionSummarizer;
import com.newrelic.jfr.tosummary.GCHeapSummarySummarizer;
import com.newrelic.jfr.tosummary.NetworkReadSummarizer;
import com.newrelic.jfr.tosummary.NetworkWriteSummarizer;
import com.newrelic.jfr.tosummary.ObjectAllocationInNewTLABSummarizer;
import com.newrelic.jfr.tosummary.ObjectAllocationOutsideTLABSummarizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ToSummaryRegistry {

  private static final List<EventToSummary> allMappers() {
    return allMappers(null);
  }

  private static final List<EventToSummary> allMappers(ThreadNameNormalizer nameNormalizer) {
    return Arrays.asList(
        new G1GarbageCollectionSummarizer(),
        new GCHeapSummarySummarizer(),
        new NetworkReadSummarizer(nameNormalizer),
        new NetworkWriteSummarizer(nameNormalizer),
        new ObjectAllocationInNewTLABSummarizer(nameNormalizer),
        new ObjectAllocationOutsideTLABSummarizer(nameNormalizer));
  }

  private final List<EventToSummary> mappers;

  private ToSummaryRegistry(List<EventToSummary> mappers) {
    this.mappers = mappers;
  }

  public static ToSummaryRegistry createDefault() {
    return new ToSummaryRegistry(allMappers());
  }

  public static ToSummaryRegistry create(ThreadNameNormalizer nameNormalizer) {
    return new ToSummaryRegistry(allMappers(nameNormalizer));
  }

  public static ToSummaryRegistry create(Collection<String> eventNames) {
    List<EventToSummary> filtered =
        allMappers()
            .stream()
            .filter(mapper -> eventNames.contains(mapper.getEventName()))
            .collect(toList());
    return new ToSummaryRegistry(filtered);
  }

  private static List<String> allEventNames() {
    return allMappers().stream().map(EventToSummary::getEventName).collect(toList());
  }

  public Stream<EventToSummary> all() {
    return mappers.stream();
  }

  public Optional<EventToSummary> get(String eventName) {
    return mappers.stream().filter(m -> m.getEventName().equals(eventName)).findFirst();
  }
}
