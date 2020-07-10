package com.newrelic.jfr.daemon;

import org.junit.jupiter.api.Test;

import static com.newrelic.jfr.daemon.DaemonConfig.DEFAULT_JMX_PORT;
import static org.junit.jupiter.api.Assertions.*;

class DaemonConfigTest {

    @Test
    void maybeEnvFound() {
        DaemonConfig.Builder builder = new DaemonConfig.Builder() {
            @Override
            String getEnv(String envKey) {
                assertEquals("myport", envKey);
                return "702";
            }
        };
        DaemonConfig result = builder.apiKey("abcd")
                .maybeEnv("myport", Integer::parseInt, builder::jmxPort)
                .build();
        assertEquals(702, result.getJmxPort());
    }

    @Test
    void maybeNotFound() {
        DaemonConfig.Builder builder = new DaemonConfig.Builder() {
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
        DaemonConfig result = builder.apiKey("abcd")
                .maybeEnv("myport", Integer::parseInt, builder::jmxPort)
                .build();
        assertEquals(DEFAULT_JMX_PORT, result.getJmxPort());

    }

    @Test
    void buildWithoutApiKey() {
        assertThrows(RuntimeException.class, () -> DaemonConfig.builder().build());
    }

}