/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SetupUtilsTest {

  @Test
  void buildCommonAttributes() {
    var mockConfig = Mockito.mock(DaemonConfig.class);
    when(mockConfig.getMonitoredAppName()).thenReturn("test_app_name");
    var attributesMap = SetupUtils.buildCommonAttributes(mockConfig).asMap();
    assertEquals("JFR", attributesMap.get(INSTRUMENTATION_NAME));
    assertEquals("JFR-Uploader", attributesMap.get(INSTRUMENTATION_PROVIDER));
    assertEquals("JFR-Uploader", attributesMap.get(COLLECTOR_NAME));
    assertEquals("test_app_name", attributesMap.get(SERVICE_NAME));
    assertEquals("test_app_name", attributesMap.get(APP_NAME));
    assertNull(attributesMap.get(SERVICE_INSTANCE_ID));
  }

  @Test
  void buildCommonAttributesWithServiceId() {
    var mockConfig = Mockito.mock(DaemonConfig.class);
    when(mockConfig.getMonitoredAppName()).thenReturn("test_app_name");
    when(mockConfig.getServiceInstanceId()).thenReturn("abcd");
    var attributesMap = SetupUtils.buildCommonAttributes(mockConfig).asMap();
    assertEquals("abcd", attributesMap.get(SERVICE_INSTANCE_ID));
  }
}
