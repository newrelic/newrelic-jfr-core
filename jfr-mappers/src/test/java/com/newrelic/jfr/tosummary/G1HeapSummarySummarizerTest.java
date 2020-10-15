package com.newrelic.jfr.tosummary;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Summary;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class G1HeapSummarySummarizerTest {

  private static Summary defaultSummary;

  @BeforeAll
  static void init() {
    defaultSummary =
        new Summary(
            "jfr:ObjectAllocationInNewTLAB.allocation",
            0,
            Duration.ofNanos(0L).toMillis(),
            Duration.ofNanos(Long.MAX_VALUE).toMillis(),
            Duration.ofNanos(Long.MIN_VALUE).toMillis(),
            Instant.now().toEpochMilli(),
            0L,
            new Attributes());
  }

  @Test
  void testSingleEventSummaryAndReset() {}

  @Test
  void testMultipleEventSummaryAndReset() {}
}
