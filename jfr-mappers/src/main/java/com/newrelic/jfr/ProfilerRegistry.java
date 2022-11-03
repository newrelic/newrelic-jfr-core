/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr;

import static java.util.stream.Collectors.toList;

import com.newrelic.jfr.profiler.EventToEventSummary;
import com.newrelic.jfr.profiler.ProfileSummarizer;
import com.newrelic.jfr.tosummary.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfilerRegistry {

  private static List<EventToEventSummary> allMappers(ThreadNameNormalizer nameNormalizer) {
    return Arrays.asList(
        ProfileSummarizer.forExecutionSample(nameNormalizer),
        ProfileSummarizer.forNativeMethodSample(nameNormalizer));
  }

  private final List<EventToEventSummary> mappers;

  private ProfilerRegistry(List<EventToEventSummary> mappers) {
    this.mappers = mappers;
  }

  /** @param nameNormalizer is required to process most metrics and flame levels. */
  public static ProfilerRegistry createDefault(ThreadNameNormalizer nameNormalizer) {
    return new ProfilerRegistry(allMappers(nameNormalizer));
  }

  /** For testing */
  static ProfilerRegistry create(Collection<String> eventNames) {
    List<EventToEventSummary> filtered =
        allMappers(null).stream()
            .filter(mapper -> eventNames.contains(mapper.getEventName()))
            .collect(toList());
    return new ProfilerRegistry(filtered);
  }

  public Stream<EventToEventSummary> all() {
    return mappers.stream();
  }

  public Optional<EventToEventSummary> get(String eventName) {
    return mappers.stream().filter(m -> m.getEventName().equals(eventName)).findFirst();
  }

  public Collection<String> getEventNames() {
    return mappers.stream()
            .map(EventToEventSummary::getEventName)
            .collect(Collectors.toSet());
  }
}
