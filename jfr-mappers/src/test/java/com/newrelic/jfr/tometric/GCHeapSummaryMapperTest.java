package com.newrelic.jfr.tometric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import org.junit.jupiter.api.Test;

class GCHeapSummaryMapperTest {

  @Test
  void testMapper() {
    var now = System.currentTimeMillis();
    Instant startTime = Instant.ofEpochMilli(now);
    var attr = new Attributes();
    long heapUsed = 300;
    long heapCommittedSize = 500;
    long reservedSize = 200;
    long heapStart = 123;
    long committedEnd = 321;
    long reservedEnd = 456;
    String when = "when";
    attr.put("when", when);
    attr.put("heapStart", heapStart);
    attr.put("committedEnd", committedEnd);
    attr.put("reservedEnd", reservedEnd);

    var gauge1 = new Gauge("jfr:GCHeapSummary.heapUsed", heapUsed, now, attr);
    var gauge2 = new Gauge("jfr:GCHeapSummary.heapCommittedSize", heapCommittedSize, now, attr);
    var gauge3 = new Gauge("jfr:GCHeapSummary.reservedSize", reservedSize, now, attr);
    List<Metric> expected = List.of(gauge1, gauge2, gauge3);

    var testClass = new GCHeapSummaryMapper();

    var recordedObject = mock(RecordedObject.class);
    when(recordedObject.getLong("committedSize")).thenReturn(heapCommittedSize);
    when(recordedObject.getLong("reservedSize")).thenReturn(reservedSize);
    when(recordedObject.getLong("start")).thenReturn(heapStart);
    when(recordedObject.getLong("committedEnd")).thenReturn(committedEnd);
    when(recordedObject.getLong("reservedEnd")).thenReturn(reservedEnd);

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getLong("heapUsed")).thenReturn(heapUsed);
    when(event.getValue("heapSpace")).thenReturn(recordedObject);
    when(event.getString("when")).thenReturn(when);

    List<? extends Metric> result = testClass.apply(event);

    assertEquals(expected, result);
  }
}
