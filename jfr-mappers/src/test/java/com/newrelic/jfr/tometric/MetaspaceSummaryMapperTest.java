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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.RecordedObjectValidators;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class MetaspaceSummaryMapperTest {
  private static MockedStatic<RecordedObjectValidators> recordedObjectValidatorsMockedStatic;

  @BeforeAll
  static void init() {
    recordedObjectValidatorsMockedStatic = Mockito.mockStatic(RecordedObjectValidators.class);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.hasField(
                    any(RecordedObject.class), anyString(), anyString()))
        .thenReturn(true);

    recordedObjectValidatorsMockedStatic
        .when(
            () ->
                RecordedObjectValidators.isRecordedObjectNull(
                    any(RecordedObject.class), anyString()))
        .thenReturn(false);
  }

  @AfterAll
  static void teardown() {
    recordedObjectValidatorsMockedStatic.close();
  }

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

    var event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(startTime);
    when(event.getString(WHEN)).thenReturn(when);
    when(event.getValue(METASPACE)).thenReturn(recordedObject);
    when(event.getValue(DATA_SPACE)).thenReturn(recordedObject);
    when(event.getValue(CLASS_SPACE)).thenReturn(recordedObject);

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
