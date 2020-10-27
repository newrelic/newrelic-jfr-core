/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;
import static com.newrelic.jfr.daemon.JFRCommonAttributes.COMMON_ATTRIBUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newrelic.telemetry.Attributes;
import java.net.UnknownHostException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JFRCommonAttributesTest {

  JFRCommonAttributes.HostInfo hostInfo;
  private DaemonConfig config;

  @BeforeEach
  void setup() {
    hostInfo =
        new JFRCommonAttributes.HostInfo() {
          @Override
          public String getLoopback() {
            return "127.0.0.1";
          }

          @Override
          public String getHost() {
            return "example.com";
          }
        };
    config = DaemonConfig.builder().monitoredAppName("myApp").apiKey("sekret").build();
  }

  @Test
  void testBuildWithEntityGuid() {
    var entityGuid = Optional.of("90210");

    var testClass = new JFRCommonAttributes(config, hostInfo);

    var expected =
        new Attributes(COMMON_ATTRIBUTES)
            .put(HOSTNAME, "example.com")
            .put("entity.guid", entityGuid.get());

    var result = testClass.build(entityGuid);
    assertEquals(expected, result);
  }

  @Test
  void testBuildWithNoEntityGuid() {
    var testClass = new JFRCommonAttributes(config, hostInfo);

    var expected =
        new Attributes(COMMON_ATTRIBUTES)
            .put(APP_NAME, config.getMonitoredAppName())
            .put(SERVICE_NAME, config.getMonitoredAppName())
            .put(HOSTNAME, "example.com");

    var result = testClass.build(Optional.empty());
    assertEquals(expected, result);
  }

  @Test
  void testHostnameLookupFails() {
    var entityGuid = Optional.of("90210");
    var hostInfo =
        new JFRCommonAttributes.HostInfo() {
          @Override
          public String getHost() throws UnknownHostException {
            throw new UnknownHostException("What is that?");
          }

          @Override
          public String getLoopback() {
            return "froot";
          }
        };

    var testClass = new JFRCommonAttributes(config, hostInfo);

    var expected =
        new Attributes(COMMON_ATTRIBUTES)
            .put(APP_NAME, config.getMonitoredAppName())
            .put(SERVICE_NAME, config.getMonitoredAppName())
            .put(HOSTNAME, "froot")
            .put("entity.guid", entityGuid.get());

    var result = testClass.build(entityGuid);
    assertEquals(expected, result);
  }
}
