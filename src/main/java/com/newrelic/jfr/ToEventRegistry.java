package com.newrelic.jfr;

import static java.util.stream.Collectors.toMap;

import com.newrelic.jfr.toevent.*;
import java.util.Collection;
import java.util.Map;

public class ToEventRegistry {

  private final Map<String, EventToEvent> mappers;

  private ToEventRegistry(Map<String, EventToEvent> mappers) {
    this.mappers = mappers;
  }

  public static ToEventRegistry createDefault() {
    var mappers =
        Map.of(
            JITCompilationMapper.EVENT_NAME, new JITCompilationMapper(),
            JVMInformationMapper.EVENT_NAME, new JVMInformationMapper(),
            JVMSystemPropertyMapper.EVENT_NAME, new JVMSystemPropertyMapper(),
            ThreadLockEventMapper.EVENT_NAME, new ThreadLockEventMapper(),
            MethodSampleMapper.EVENT_NAME, new MethodSampleMapper(),
            MethodSampleMapper.NATIVE_EVENT_NAME, new MethodSampleMapper());
    return new ToEventRegistry(mappers);
  }

  public static ToEventRegistry create(Collection<String> eventNames) {
    var all = createDefault();
    var filtered =
        all.getMappers()
            .entrySet()
            .stream()
            .filter(e -> eventNames.contains(e.getKey()))
            .collect(toMap(n -> n.getKey(), n -> n.getValue()));
    return new ToEventRegistry(filtered);
  }

  private Map<String, EventToEvent> getMappers() {
    return mappers;
  }

  public EventToEvent get(String eventName) {
    return mappers.get(eventName);
  }
}
