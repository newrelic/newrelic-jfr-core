package com.newrelic.jfr.tometric;

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
    attr.put("when", when);

    double used = 10;
    double committed = 30;
    double reserved = 20;

    List<Metric> expected = new ArrayList<>(9);
    expected.addAll(
        generateMetrics(
            MetaspaceSummaryMapper.METASPACE_KEY, used, committed, reserved, now, attr));
    expected.addAll(
        generateMetrics(
            MetaspaceSummaryMapper.DATA_SPACE_KEY, used, committed, reserved, now, attr));
    expected.addAll(
        generateMetrics(
            MetaspaceSummaryMapper.CLASS_SPACE_KEY, used, committed, reserved, now, attr));

    var testClass = new MetaspaceSummaryMapper();

    var recordedObject = mock(RecordedObject.class);
    when(recordedObject.getDouble("used")).thenReturn(used);
    when(recordedObject.getDouble("committed")).thenReturn(committed);
    when(recordedObject.getDouble("reserved")).thenReturn(reserved);

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getString("when")).thenReturn(when);
    when(event.getValue("metaspace")).thenReturn(recordedObject);
    when(event.getValue("dataSpace")).thenReturn(recordedObject);
    when(event.getValue("classSpace")).thenReturn(recordedObject);

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
            MetaspaceSummaryMapper.NR_METRIC_PREFIX + metricName + ".committed",
            committed,
            now,
            attr);
    var gauge2 =
        new Gauge(MetaspaceSummaryMapper.NR_METRIC_PREFIX + metricName + ".used", used, now, attr);
    var gauge3 =
        new Gauge(
            MetaspaceSummaryMapper.NR_METRIC_PREFIX + metricName + ".reserved",
            reserved,
            now,
            attr);
    return List.of(gauge1, gauge2, gauge3);
  }
}
