package com.newrelic.jfr.tometric;

import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.CLASS_SPACE;
import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.COMMITTED;
import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.DATA_SPACE;
import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.DOT_DELIMITER;
import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.METASPACE;
import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.RESERVED;
import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.USED;
import static com.newrelic.jfr.tometric.MetaspaceSummaryMapper.WHEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import org.junit.jupiter.api.Test;

public class MetaspaceSummaryMapperTest {

  @Test
  void testMapper() {
    var now = System.currentTimeMillis();
    Instant startTime = Instant.ofEpochMilli(now);

    var attr = new Attributes();
    String when = "when";
    attr.put(WHEN, when);

    double used = 10;
    double committed = 30;
    double reserved = 20;

    List<Metric> expected = new ArrayList<>(9);
    expected.addAll(
        generateMetrics(MetaspaceSummaryMapper.METASPACE, used, committed, reserved, now, attr));
    expected.addAll(
        generateMetrics(MetaspaceSummaryMapper.DATA_SPACE, used, committed, reserved, now, attr));
    expected.addAll(
        generateMetrics(MetaspaceSummaryMapper.CLASS_SPACE, used, committed, reserved, now, attr));

    var testClass = new MetaspaceSummaryMapper();

    var recordedObject = mock(RecordedObject.class);
    when(recordedObject.getDouble(USED)).thenReturn(used);
    when(recordedObject.getDouble(COMMITTED)).thenReturn(committed);
    when(recordedObject.getDouble(RESERVED)).thenReturn(reserved);
    when(recordedObject.hasField(USED)).thenReturn(true);
    when(recordedObject.hasField(COMMITTED)).thenReturn(true);
    when(recordedObject.hasField(RESERVED)).thenReturn(true);

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getString(WHEN)).thenReturn(when);
    when(event.getValue(METASPACE)).thenReturn(recordedObject);
    when(event.getValue(DATA_SPACE)).thenReturn(recordedObject);
    when(event.getValue(CLASS_SPACE)).thenReturn(recordedObject);
    when(event.hasField(WHEN)).thenReturn(true);
    when(event.hasField(METASPACE)).thenReturn(true);
    when(event.hasField(DATA_SPACE)).thenReturn(true);
    when(event.hasField(CLASS_SPACE)).thenReturn(true);

    List<? extends Metric> result = testClass.apply(event);

    assertEquals(expected, result);
  }

  List<Metric> generateMetrics(
      String metricName,
      double used,
      double committed,
      double reserved,
      long now,
      Attributes attr) {
    var gauge1 =
        new Gauge(
            MetaspaceSummaryMapper.NR_METRIC_PREFIX + metricName + DOT_DELIMITER + COMMITTED,
            committed,
            now,
            attr);
    var gauge2 =
        new Gauge(
            MetaspaceSummaryMapper.NR_METRIC_PREFIX + metricName + DOT_DELIMITER + USED,
            used,
            now,
            attr);
    var gauge3 =
        new Gauge(
            MetaspaceSummaryMapper.NR_METRIC_PREFIX + metricName + DOT_DELIMITER + RESERVED,
            reserved,
            now,
            attr);
    return List.of(gauge1, gauge2, gauge3);
  }
}
