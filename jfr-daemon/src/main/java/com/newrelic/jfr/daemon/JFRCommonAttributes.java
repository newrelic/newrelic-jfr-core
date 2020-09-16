/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import com.newrelic.telemetry.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Optional;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.COLLECTOR_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.HOSTNAME;
import static com.newrelic.jfr.daemon.AttributeNames.INSTRUMENTATION_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.INSTRUMENTATION_PROVIDER;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;

public class JFRCommonAttributes {

    private static final Logger logger = LoggerFactory.getLogger(JFRCommonAttributes.class);
    private static final Attributes COMMON_ATTRIBUTES =
            new Attributes()
                    .put(INSTRUMENTATION_NAME, "JFR")
                    .put(INSTRUMENTATION_PROVIDER, "JFR-Uploader")
                    .put(COLLECTOR_NAME, "JFR-Uploader");
    private final DaemonConfig config;

    public JFRCommonAttributes(DaemonConfig config) {
        this.config = config;
    }

    public Attributes build(Optional<String> entityGuid){
        var hostname = findHostname();
        var attr =
                COMMON_ATTRIBUTES
                        .put(APP_NAME, config.getMonitoredAppName())
                        .put(SERVICE_NAME, config.getMonitoredAppName())
                        .put(HOSTNAME, hostname);
        entityGuid.ifPresent(guid -> attr.put("entity.guid", guid));
        return attr;
    }

    private static String findHostname() {
        try {
            return InetAddress.getLocalHost().toString();
        } catch (Throwable e) {
            var loopback = InetAddress.getLoopbackAddress().toString();
            logger.error(
                    "Unable to get localhost IP, defaulting to loopback address," + loopback + ".", e);
            return loopback;
        }
    }


}
