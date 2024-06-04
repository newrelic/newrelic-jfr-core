/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SetupUtilsTest {

  @Test
  void buildCommonAttributes() {
    var mockConfig = mock(DaemonConfig.class);
    when(mockConfig.getMonitoredAppName()).thenReturn("test_app_name");
    var attributesMap = SetupUtils.buildCommonAttributes(mockConfig).asMap();
    assertEquals("JFR", attributesMap.get(INSTRUMENTATION_NAME));
    assertEquals("JFR-Uploader", attributesMap.get(INSTRUMENTATION_PROVIDER));
    assertEquals("JFR-Uploader", attributesMap.get(COLLECTOR_NAME));
    assertEquals("test_app_name", attributesMap.get(SERVICE_NAME));
    assertEquals("test_app_name", attributesMap.get(APP_NAME));
    assertNotNull(attributesMap.get(HOSTNAME));
  }

  @Test
  void testBuildCommonAttributesWithSpecificHostname() {
    DaemonConfig config = mock(DaemonConfig.class);
    when(config.getHostname()).thenReturn("test_hostname");
    when(config.getMonitoredAppName()).thenReturn("test_app_name");

    Attributes attributes = SetupUtils.buildCommonAttributes(config);

    assertNotNull(attributes);
    assertEquals("JFR", attributes.asMap().get(INSTRUMENTATION_NAME));
    assertEquals("JFR-Uploader", attributes.asMap().get(INSTRUMENTATION_PROVIDER));
    assertEquals("JFR-Uploader", attributes.asMap().get(COLLECTOR_NAME));
    assertEquals("test_app_name", attributes.asMap().get(SERVICE_NAME));
    assertEquals("test_app_name", attributes.asMap().get(APP_NAME));
    assertEquals("test_hostname", attributes.asMap().get(HOSTNAME));
    assertEquals("test_app_name", attributes.asMap().get(APP_NAME));
    assertEquals("test_app_name", attributes.asMap().get(SERVICE_NAME));
  }

  @Test
  void testBuildCommonAttributesWithNullConfigValueLocalHostResolution() throws Exception {
    DaemonConfig config = mock(DaemonConfig.class);
    when(config.getHostname()).thenReturn(null);
    when(config.getMonitoredAppName()).thenReturn("test_app_name");

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.getHostName()).thenReturn("resolved_hostname");

    try (MockedStatic<InetAddress> inetAddressMockedStatic =
        Mockito.mockStatic(InetAddress.class)) {
      inetAddressMockedStatic.when(InetAddress::getLocalHost).thenReturn(inetAddress);

      Attributes attributes = SetupUtils.buildCommonAttributes(config);

      assertNotNull(attributes);
      assertEquals("JFR", attributes.asMap().get(INSTRUMENTATION_NAME));
      assertEquals("JFR-Uploader", attributes.asMap().get(INSTRUMENTATION_PROVIDER));
      assertEquals("JFR-Uploader", attributes.asMap().get(COLLECTOR_NAME));
      assertEquals("test_app_name", attributes.asMap().get(SERVICE_NAME));
      assertEquals("test_app_name", attributes.asMap().get(APP_NAME));
      assertEquals("resolved_hostname", attributes.asMap().get(HOSTNAME));
      assertEquals("test_app_name", attributes.asMap().get(APP_NAME));
      assertEquals("test_app_name", attributes.asMap().get(SERVICE_NAME));
    }
    ;
  }

  @Test
  void testBuildCommonAttributesWithNullConfigValueLoopbackResolution() throws Exception {
    DaemonConfig config = mock(DaemonConfig.class);
    when(config.getHostname()).thenReturn(null);
    when(config.getMonitoredAppName()).thenReturn("test_app_name");

    InetAddress loopbackAddress = InetAddress.getLoopbackAddress();

    try (MockedStatic<InetAddress> inetAddressMockedStatic =
        Mockito.mockStatic(InetAddress.class)) {
      inetAddressMockedStatic
          .when(InetAddress::getLocalHost)
          .thenAnswer(
              invocationOnMock -> {
                throw new RuntimeException("Hostname resolution failed");
              });
      inetAddressMockedStatic.when(InetAddress::getLoopbackAddress).thenReturn(loopbackAddress);

      Attributes attributes = SetupUtils.buildCommonAttributes(config);

      assertNotNull(attributes);
      assertEquals("JFR", attributes.asMap().get(INSTRUMENTATION_NAME));
      assertEquals("JFR-Uploader", attributes.asMap().get(INSTRUMENTATION_PROVIDER));
      assertEquals("JFR-Uploader", attributes.asMap().get(COLLECTOR_NAME));
      assertEquals("test_app_name", attributes.asMap().get(SERVICE_NAME));
      assertEquals("test_app_name", attributes.asMap().get(APP_NAME));
      assertEquals(loopbackAddress.toString(), attributes.asMap().get(HOSTNAME));
      assertEquals("test_app_name", attributes.asMap().get(APP_NAME));
      assertEquals("test_app_name", attributes.asMap().get(SERVICE_NAME));
    }
  }
}
