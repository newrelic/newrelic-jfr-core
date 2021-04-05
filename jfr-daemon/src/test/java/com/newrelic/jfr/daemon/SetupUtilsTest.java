/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.AttributeNames.COLLECTOR_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.INSTRUMENTATION_PROVIDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SetupUtilsTest {

  @Test
  void buildCommonAttributes() {
    var mockConfig = Mockito.mock(DaemonConfig.class);
    var attributesMap = SetupUtils.buildCommonAttributes(mockConfig).asMap();
    assertEquals("JFR", attributesMap.get(INSTRUMENTATION_NAME));
    assertEquals("JFR-Uploader", attributesMap.get(INSTRUMENTATION_PROVIDER));
    assertEquals("JFR-Uploader", attributesMap.get(COLLECTOR_NAME));
    assertNotNull(attributesMap.get(HOSTNAME));
  }
}
