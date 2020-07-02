package com.newrelic.jfr;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.toevent.EventToEvent;
import com.newrelic.jfr.tometric.EventToMetric;
import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.events.Event;
import com.newrelic.telemetry.events.EventBatch;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.Summary;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileToBufferedTelemetryTest {

  private long start;
  private Instant time1;
  private Instant time2;
  private Attributes commonAttributes;
  private RecordedEvent event1;
  private RecordedEvent event2;
  private ToMetricRegistry metricRegistry;
  private ToEventRegistry eventRegistry;
  private ToSummaryRegistry summaryRegistry;
  private RecordingFile recordingFile;

  @BeforeEach
  void setup() {
    start = System.currentTimeMillis();
    time1 = Instant.ofEpochMilli(start + 1500);
    time2 = Instant.ofEpochMilli(start + 2500);
    commonAttributes = new Attributes().put("common", "atts");
    metricRegistry = mock(ToMetricRegistry.class);
    eventRegistry = mock(ToEventRegistry.class);
    summaryRegistry = mock(ToSummaryRegistry.class);
    recordingFile = mock(RecordingFile.class);
    event1 = mock(RecordedEvent.class);
    event2 = mock(RecordedEvent.class);

    when(event1.getStartTime()).thenReturn(time1);
    when(event2.getStartTime()).thenReturn(time2);
    when(event1.getEventType()).thenReturn(mock(EventType.class));
    when(event2.getEventType()).thenReturn(mock(EventType.class));
  }

  @Test
  void testConvertHappyPath() throws Exception {

    var expectedMetric = new Count("myCount", 123.44f, start - 17, start - 11, commonAttributes);
    var expectedEvent = new Event("myType", commonAttributes);
    var expectedSummary = new Summary("fff", 23, 42, 55, 77, start, start + 55, commonAttributes);

    var metricConverter1 = makeMetricConverter(null, null); // match nothing
    var metricConverter2 = makeMetricConverter(event1, expectedMetric);
    var eventConverter1 = makeEventConverter(null, null);
    var eventConverter2 = makeEventConverter(event2, expectedEvent);
    var summaryConverter = mock(EventToSummary.class);

    var expectedMetricBatch =
        new MetricBatch(List.of(expectedMetric, expectedSummary), commonAttributes);
    var expectedEventBatch = new EventBatch(List.of(expectedEvent), commonAttributes);

    when(recordingFile.hasMoreEvents()).thenReturn(true, true, true, false);
    when(recordingFile.readEvent()).thenReturn(event1, null, event2);
    when(metricRegistry.all()).then(x -> Stream.of(metricConverter1, metricConverter2));
    when(eventRegistry.all()).then(x -> Stream.of(eventConverter1, eventConverter2));
    when(summaryRegistry.all()).then(x -> Stream.of(summaryConverter));
    when(summaryConverter.test(event1)).thenReturn(true);
    when(summaryConverter.summarizeAndReset()).thenReturn(Stream.of(expectedSummary));

    var testClass =
        FileToBufferedTelemetry.builder()
            .commonAttributes(commonAttributes)
            .metricMappers(metricRegistry)
            .eventMapper(eventRegistry)
            .summaryMappers(summaryRegistry)
            .build();

    var onlyAfter = Instant.ofEpochMilli(start);
    var result = testClass.convert(recordingFile, onlyAfter, "x");

    assertEquals(time2, result.getLastSeen());
    assertEquals(expectedMetricBatch, result.getBufferedTelemetry().createMetricBatch());
    assertEquals(expectedEventBatch, result.getBufferedTelemetry().createEventBatch());
    verify(summaryConverter).accept(event1);
    verify(summaryConverter, never()).accept(event2);
  }

  @Test
  void testMappersCanExplode() throws IOException {
    var expectedMetric = new Count("myCount", 123.44f, start - 17, start - 11, commonAttributes);
    var expectedEvent = new Event("myType", commonAttributes);
    var expectedMetricBatch = new MetricBatch(List.of(expectedMetric), commonAttributes);
    var expectedEventBatch = new EventBatch(List.of(expectedEvent), commonAttributes);

    var metricConverter = makeMetricConverter(event1, expectedMetric);
    var eventConverter = mock(EventToEvent.class);

    when(recordingFile.hasMoreEvents()).thenReturn(true, true, false);
    when(recordingFile.readEvent()).thenReturn(event1, event2);
    when(metricRegistry.all()).then(x -> Stream.of(metricConverter));
    when(eventRegistry.all()).then(x -> Stream.of(eventConverter));
    when(summaryRegistry.all()).then(x -> Stream.empty());

    when(eventConverter.test(isA(RecordedEvent.class))).thenReturn(true);
    when(eventConverter.apply(event1)).thenThrow(new RuntimeException("I don't like that one"));
    when(eventConverter.apply(event2)).thenReturn(singletonList(expectedEvent));

    var testClass =
        FileToBufferedTelemetry.builder()
            .commonAttributes(commonAttributes)
            .metricMappers(metricRegistry)
            .eventMapper(eventRegistry)
            .summaryMappers(summaryRegistry)
            .build();

    var onlyAfter = Instant.ofEpochMilli(start);
    var result = testClass.convert(recordingFile, onlyAfter, "x");

    assertEquals(time2, result.getLastSeen());
    assertEquals(expectedMetricBatch, result.getBufferedTelemetry().createMetricBatch());
    assertEquals(expectedEventBatch, result.getBufferedTelemetry().createEventBatch());
  }

  private EventToMetric makeMetricConverter(RecordedEvent eventWeWant, Metric metricToReturn) {
    var result = mock(EventToMetric.class);
    if (eventWeWant != null) {
      when(result.test(eventWeWant)).thenReturn(true);
    }
    doReturn(singletonList(metricToReturn)).when(result).apply(eventWeWant);
    return result;
  }

  private EventToEvent makeEventConverter(RecordedEvent eventWeWant, Event eventToReturn) {
    var result = mock(EventToEvent.class);
    if (eventWeWant != null) {
      when(result.test(eventWeWant)).thenReturn(true);
    }
    doReturn(singletonList(eventToReturn)).when(result).apply(eventWeWant);
    return result;
  }
}
