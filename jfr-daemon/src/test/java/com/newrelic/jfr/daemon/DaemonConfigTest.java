package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.DaemonConfig.DEFAULT_JMX_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class DaemonConfigTest {

  @Test
  void maybeEnvFound() {
    DaemonConfig.Builder builder =
        new DaemonConfig.Builder() {
          @Override
          String getEnv(String envKey) {
            assertEquals("myport", envKey);
            return "702";
          }
        };
    DaemonConfig result =
        builder.apiKey("abcd").maybeEnv("myport", Integer::parseInt, builder::jmxPort).build();
    assertEquals(702, result.getJmxPort());
  }

  @Test
  void maybeNotFound() {
    DaemonConfig.Builder builder =
        new DaemonConfig.Builder() {
          @Override
          String getEnv(String envKey) {
            return null;
          }

          @Override
          public DaemonConfig.Builder jmxPort(int port) {
            fail("port should not have been set when not in the environment");
            throw new RuntimeException("should not happen");
          }
        };
    DaemonConfig result =
        builder.apiKey("abcd").maybeEnv("myport", Integer::parseInt, builder::jmxPort).build();
    assertEquals(DEFAULT_JMX_PORT, result.getJmxPort());
  }

  @Test
  void buildWithoutApiKey() {
    assertThrows(RuntimeException.class, () -> DaemonConfig.builder().build());
  }

  @Test
  void streamOverJmx() {
    var config = DaemonConfig.builder().apiKey("a").build();
    assertTrue(config.streamFromJmx());
    config = DaemonConfig.builder().apiKey("a").useSharedFilesystem(true).build();
    assertFalse(config.streamFromJmx());
    config = DaemonConfig.builder().apiKey("a").useSharedFilesystem(false).build();
    assertTrue(config.streamFromJmx());
  }

  @Test
  void assigningServiceIdFromOtelResourceString() {
    DaemonConfig config =
        DaemonConfig.builder()
            .apiKey("a")
            .otelResourceAttributes(
                "service.name=myApp,deployment.environment=production,service.version=1.0,service.instance.id=abcd")
            .serviceInstanceId("12345")
            .build();
    assertEquals("abcd", config.getServiceInstanceId());

    config =
        DaemonConfig.builder()
            .apiKey("a")
            .otelResourceAttributes(
                "service.name=myApp,deployment.environment=production,service.version=1.0")
            .serviceInstanceId("12345")
            .build();
    assertEquals("12345", config.getServiceInstanceId());
  }

  @Test
  void assigningServiceIdExplicitly() {
    DaemonConfig config = DaemonConfig.builder().apiKey("a").serviceInstanceId("12345").build();
    assertEquals("12345", config.getServiceInstanceId());

    config = DaemonConfig.builder().apiKey("a").serviceInstanceId(null).build();
    assertNull(config.getServiceInstanceId());
    config = DaemonConfig.builder().apiKey("a").build();
    assertNull(config.getServiceInstanceId());
  }
}
