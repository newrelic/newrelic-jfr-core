/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr;

import static java.util.stream.Collectors.*;

import com.newrelic.jfr.toevent.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ToEventRegistry {

  private static final List<EventToEvent> ALL_MAPPERS =
      Arrays.asList(
          new JITCompilationMapper(),
          new JVMInformationMapper(),
          new JVMSystemPropertyMapper(),
          new ThreadLockEventMapper(),
          new ValhallaVBCDetector(),
          MethodSampleMapper.forExecutionSample(),
          MethodSampleMapper.forNativeMethodSample());

  private final List<EventToEvent> mappers;

  private ToEventRegistry(List<EventToEvent> mappers) {
    this.mappers = mappers;
  }

  public static ToEventRegistry createDefault() {
    return create(allEventNames());
  }

  public static ToEventRegistry create(Collection<String> eventNames) {
    List<EventToEvent> filtered =
        ALL_MAPPERS
            .stream()
            .filter(mapper -> eventNames.contains(mapper.getEventName()))
            .collect(toList());
    return new ToEventRegistry(filtered);
  }

  private List<EventToEvent> getMappers() {
    return mappers;
  }

  private static List<String> allEventNames() {
    return ALL_MAPPERS.stream().map(EventToEvent::getEventName).collect(toList());
  }

  /** @return a stream of all EventToEvent entries in this registry. */
  public Stream<EventToEvent> all() {
    return mappers.stream();
  }

  /**
   * Returns an optional EventToEvent containing the mapper with the given JFR event name. If the
   * event is not known to this registry, the returned Optional will be empty.
   *
   * @param eventName - the JFR name of the event to find
   * @return - an optional EventToEvent.
   */
  public Optional<EventToEvent> get(String eventName) {
    return mappers.stream().filter(toEvent -> toEvent.getEventName().equals(eventName)).findFirst();
  }
}
