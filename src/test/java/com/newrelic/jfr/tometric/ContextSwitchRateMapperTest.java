package com.newrelic.jfr.tometric;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Instant;
import java.util.List;
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;

class ContextSwitchRateMapperTest {

  @Test
  void testApply() {
    double switchRate = 91.5;
    Instant timestamp = Instant.now();

    Gauge expectedGauge =
        new Gauge(
            "jfr:ThreadContextSwitchRate", switchRate, timestamp.toEpochMilli(), new Attributes());

    RecordedEvent event = mock(RecordedEvent.class);
    when(event.getStartTime()).thenReturn(timestamp);
    when(event.getDouble("switchRate")).thenReturn(switchRate);

    ContextSwitchRateMapper mapper = new ContextSwitchRateMapper();
    List<? extends Metric> result = mapper.apply(event);

    assertEquals(List.of(expectedGauge), result);
  }
}
