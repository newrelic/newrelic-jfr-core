/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

public class EnvironmentVars {

  public static final String INSERT_API_KEY = "INSIGHTS_INSERT_KEY";
  public static final String METRICS_INGEST_URI = "METRICS_INGEST_URI";
  public static final String EVENTS_INGEST_URI = "EVENTS_INGEST_URI";
  public static final String ENV_APP_NAME = "NEW_RELIC_APP_NAME";
  public static final String REMOTE_JMX_HOST = "REMOTE_JMX_HOST";
  public static final String REMOTE_JMX_PORT = "REMOTE_JMX_PORT";
  public static final String JFR_SHARED_FILESYSTEM = "JFR_SHARED_FILESYSTEM";
  public static final String JFR_USE_LICENSE_KEY = "JFR_USE_LICENSE_KEY";
  public static final String AUDIT_LOGGING = "AUDIT_LOGGING";
  public static final String USE_LICENSE_KEY = "USE_LICENSE_KEY";

  private EnvironmentVars() {}
}
