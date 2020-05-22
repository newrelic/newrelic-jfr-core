package com.newrelic.jfr.tometric;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public class ContextSwitchRateMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.ThreadContextSwitchRate";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    var timestamp = ev.getStartTime().toEpochMilli();
    var attr = new Attributes();
    return List.of(
        new Gauge("jfr:ThreadContextSwitchRate", ev.getDouble("switchRate"), timestamp, attr));
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.of(1, SECONDS.toChronoUnit()));
  }
}
