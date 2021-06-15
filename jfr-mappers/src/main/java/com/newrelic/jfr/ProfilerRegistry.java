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
import java.util.stream.Stream;

public class ProfilerRegistry {

  private static final List<EventToEventSummary> ALL_MAPPERS =
      Arrays.asList(
          ProfileSummarizer.forExecutionSample(), ProfileSummarizer.forNativeMethodSample());

  private final List<EventToEventSummary> mappers;

  private ProfilerRegistry(List<EventToEventSummary> mappers) {
    this.mappers = mappers;
  }

  public static ProfilerRegistry createDefault() {
    return new ProfilerRegistry(ALL_MAPPERS);
  }

  public static ProfilerRegistry create(Collection<String> eventNames) {
    List<EventToEventSummary> filtered =
        ALL_MAPPERS
            .stream()
            .filter(mapper -> eventNames.contains(mapper.getEventName()))
            .collect(toList());
    return new ProfilerRegistry(filtered);
  }

  private static List<String> allEventNames() {
    return ALL_MAPPERS.stream().map(EventToEventSummary::getEventName).collect(toList());
  }

  public Stream<EventToEventSummary> all() {
    return mappers.stream();
  }

  public Optional<EventToEventSummary> get(String eventName) {
    return mappers.stream().filter(m -> m.getEventName().equals(eventName)).findFirst();
  }
}
