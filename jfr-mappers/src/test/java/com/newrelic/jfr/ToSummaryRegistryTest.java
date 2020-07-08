package com.newrelic.jfr;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newrelic.jfr.tosummary.EventToSummary;
import com.newrelic.jfr.tosummary.G1GarbageCollectionSummarizer;
import com.newrelic.jfr.tosummary.NetworkReadSummarizer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToSummaryRegistryTest {

  @Test
  void testUnknownNamesDropped() {
    var names =
        List.of(
            "unknown1",
            G1GarbageCollectionSummarizer.EVENT_NAME,
            "unknown2",
            NetworkReadSummarizer.EVENT_NAME,
            "unknown3");

    var expected =
        List.of(G1GarbageCollectionSummarizer.EVENT_NAME, NetworkReadSummarizer.EVENT_NAME);
    ToSummaryRegistry registry = ToSummaryRegistry.create(names);

    var actual = registry.all().map(EventToSummary::getEventName).collect(toList());
    assertEquals(expected, actual);
  }

  @Test
  void testGetUnknown() {
    ToSummaryRegistry registry = ToSummaryRegistry.createDefault();
    assertTrue(registry.get("NOT gonna find me").isEmpty());
  }

  @Test
  void testGetKnown() {
    ToSummaryRegistry registry = ToSummaryRegistry.createDefault();
    assertTrue(registry.get(NetworkReadSummarizer.EVENT_NAME).isPresent());
  }
}
