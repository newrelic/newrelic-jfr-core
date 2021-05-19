/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.ProfilerRegistry;
import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.toevent.EventToEvent;
import com.newrelic.jfr.tometric.EventToMetric;
import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Summary;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class EventConverterTest {

  @Test
  void testBufferEventsHappyPath() {
    var event = new Event("foo", new Attributes().put("x", "y"), 12354);
    var metric = new Gauge("shimmy", 12.45, 1234567L, new Attributes().put("a", "b"));
    var summary = new Summary("dracula", 1, 23, 24, 25, 101, 102, new Attributes().put("z", "q"));

    var e1 = makeEvent("one");
    var e2 = makeEvent("two");
    var e3 = makeEvent("three");
    var attrs = new Attributes().put("foo", "bar");
    var toEventRegistry = mock(ToEventRegistry.class);
    var toMetricRegistry = mock(ToMetricRegistry.class);
    var toSummaryRegistry = mock(ToSummaryRegistry.class);
    var profilerRegistry = mock(ProfilerRegistry.class);
    var buffer = mock(RecordedEventBuffer.class);
    var eventToEvent = mock(EventToEvent.class);
    var eventToMetric = mock(EventToMetric.class);
    var eventToSummary = mock(EventToSummary.class);

    when(buffer.drainToStream()).thenReturn(Stream.of(e1, e2, e3));

    when(toEventRegistry.all()).thenAnswer(x -> Stream.of(eventToEvent));
    when(eventToEvent.test(e1)).thenReturn(true);
    when(eventToEvent.apply(e1)).thenReturn(List.of(event));

    when(toMetricRegistry.all()).thenAnswer(x -> Stream.of(eventToMetric));
    when(eventToMetric.test(e2)).thenReturn(true);
    doReturn(List.of(metric)).when(eventToMetric).apply(e2);

    when(toSummaryRegistry.all()).thenAnswer(x -> Stream.of(eventToSummary));
    when(eventToSummary.test(e3)).thenReturn(true);
    doReturn(Stream.of(summary)).when(eventToSummary).summarize();

    var testClass =
        new EventConverter(
            attrs, toMetricRegistry, toSummaryRegistry, toEventRegistry, profilerRegistry);

    var result = testClass.convert(buffer);
    var eventBatch = result.createEventBatch();
    assertEquals(1, eventBatch.size());
    assertEquals(event, eventBatch.getTelemetry().iterator().next());
    var metricBatch = result.createMetricBatch();
    assertEquals(2, metricBatch.getTelemetry().size());
    assertEquals(metric, new ArrayList<>(metricBatch.getTelemetry()).get(0));
    assertEquals(summary, new ArrayList<>(metricBatch.getTelemetry()).get(1));
  }

  @Test
  void testConversionException() {
    var e1 = makeEvent("one");
    var attrs = new Attributes().put("foo", "bar");
    var toMetricRegistry = mock(ToMetricRegistry.class);
    var toSummaryRegistry = mock(ToSummaryRegistry.class);
    var profilerRegistry = mock(ProfilerRegistry.class);

    var buffer = mock(RecordedEventBuffer.class);

    when(buffer.drainToStream()).thenReturn(Stream.of(e1));
    when(toSummaryRegistry.all()).thenAnswer(x -> Stream.empty());

    when(toMetricRegistry.all()).thenThrow(new RuntimeException("Whoops"));

    var testClass =
        new EventConverter(attrs, toMetricRegistry, toSummaryRegistry, null, profilerRegistry);

    var result = testClass.convert(buffer);
    assertNotNull(result);
  }

  private RecordedEvent makeEvent(String name) {
    var result = mock(RecordedEvent.class);
    var eventType = mock(EventType.class);
    when(result.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn(name);
    when(eventType.getDescription()).thenReturn("mock");
    return result;
  }
}
