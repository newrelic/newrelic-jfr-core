package com.newrelic.jfr.daemon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.ToEventRegistry;
import com.newrelic.jfr.ToMetricRegistry;
import com.newrelic.jfr.ToSummaryRegistry;
import com.newrelic.jfr.daemon.buffer.BufferedTelemetry;
import com.newrelic.jfr.toevent.EventToEvent;
import com.newrelic.jfr.tometric.EventToMetric;
import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.MetricBatch;
import java.util.List;
import java.util.stream.Stream;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordedEventToTelemetryTest {

  @Mock ToEventRegistry eventRegistry;
  @Mock ToMetricRegistry metricRegistry;
  @Mock ToSummaryRegistry summaryRegistry;
  @Mock RecordedEvent event;
  @Mock EventToEvent eventMapper2;
  @Mock EventToEvent eventMapper1;
  @Mock EventToMetric metricMapper1;
  @Mock EventToMetric metricMapper2;
  @Mock EventToSummary summaryMapper1;
  @Mock EventToSummary summaryMapper2;

  @Test
  void testProcessEventHappyPath() {
    var buffer = BufferedTelemetry.create(new Attributes());
    var outEvent = new Event("foo", new Attributes());
    var expectedEvents = new EventBatch(List.of(outEvent));
    var outGauge = new Gauge("bar", 12.123, 101, new Attributes());
    var expectedMetrics = new MetricBatch(List.of(outGauge), new Attributes());

    when(eventRegistry.all()).thenReturn(Stream.of(eventMapper1, eventMapper2));
    when(metricRegistry.all()).thenReturn(Stream.of(metricMapper1, metricMapper2));
    when(summaryRegistry.all()).thenReturn(Stream.of(summaryMapper1, summaryMapper2));
    when(eventMapper1.test(event)).thenReturn(false);
    when(eventMapper2.test(event)).thenReturn(true);
    when(eventMapper2.apply(event)).thenReturn(List.of(outEvent));
    when(metricMapper1.test(event)).thenReturn(false);
    when(metricMapper2.test(event)).thenReturn(true);
    doReturn(List.of(outGauge)).when(metricMapper2).apply(event);
    when(summaryMapper1.test(event)).thenReturn(false);
    when(summaryMapper2.test(event)).thenReturn(true);

    var testClass =
        RecordedEventToTelemetry.builder()
            .eventMappers(eventRegistry)
            .metricMappers(metricRegistry)
            .summaryMappers(summaryRegistry)
            .build();

    testClass.processEvent(event, buffer);
    assertEquals(expectedEvents, buffer.createEventBatch());
    assertEquals(expectedMetrics, buffer.createMetricBatch());
    verify(eventMapper1, never()).apply(event);
    verify(metricMapper1, never()).apply(event);
    verify(summaryMapper1, never()).accept(event);
    verify(summaryMapper2).accept(event);
  }

  @Test
  void testNullEvent() {
    var testClass = RecordedEventToTelemetry.builder().build();
    testClass.processEvent(null, null);
    // Just makes sure that we didn't NPE, since nothing is plumbed
  }

  @Test
  void testConversionExceptionsAreTrapped() {
    var buffer = mock(BufferedTelemetry.class);
    var eventType = mock(EventType.class);
    when(event.getEventType()).thenReturn(eventType);
    when(eventType.getName()).thenReturn("pain");
    when(eventType.getDescription()).thenReturn("real pain");
    when(metricRegistry.all()).thenReturn(Stream.of(metricMapper1, metricMapper2));
    when(metricMapper1.test(event)).thenReturn(true);
    when(metricMapper1.apply(event)).thenThrow(new RuntimeException("A very unhappy mapper"));

    var testClass =
        RecordedEventToTelemetry.builder()
            .eventMappers(eventRegistry)
            .metricMappers(metricRegistry)
            .summaryMappers(summaryRegistry)
            .build();

    testClass.processEvent(event, buffer);
    // Note: If the metric mapper explodes, we don't try and convert with the other types
    verifyNoInteractions(eventRegistry);
    verifyNoInteractions(summaryRegistry);
    verifyNoInteractions(buffer);
  }
}
