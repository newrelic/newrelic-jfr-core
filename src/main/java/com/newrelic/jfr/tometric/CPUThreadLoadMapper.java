package com.newrelic.jfr.tometric;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.newrelic.jfr.Workarounds;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

// jdk.ThreadCPULoad {
//        startTime = 10:37:24.287
//        user = 0.00%
//        system = 0.00%
//        eventThread = "C1 CompilerThread0" (javaThreadId = 8)
//        }
public class CPUThreadLoadMapper implements EventToMetric {
  public static final String EVENT_NAME = "jdk.ThreadCPULoad";

  @Override
  public List<? extends Metric> apply(RecordedEvent ev) {
    var possibleThreadName = Workarounds.getThreadName(ev);
    if (possibleThreadName.isPresent()) {
      var threadName = possibleThreadName.get();
      var timestamp = ev.getStartTime().toEpochMilli();
      var attr = new Attributes().put("thread.name", threadName);

      // Do we need to throttle these events somehow? Or just send everything?
      return List.of(
          new Gauge("jfr:ThreadCPULoad.user", ev.getDouble("user"), timestamp, attr),
          new Gauge("jfr:ThreadCPULoad.system", ev.getDouble("system"), timestamp, attr));
    }
    return List.of();
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
