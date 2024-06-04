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
  public static final String AUDIT_LOGGING = "AUDIT_LOGGING";
  public static final String USE_LICENSE_KEY = "USE_LICENSE_KEY";
  public static final String PROXY_HOST = "PROXY_HOST";
  public static final String PROXY_PORT = "PROXY_PORT";
  public static final String PROXY_USER = "PROXY_USER";
  public static final String PROXY_PASSWORD = "PROXY_PASSWORD";
  public static final String PROXY_SCHEME = "PROXY_SCHEME";
  public static final String THREAD_NAME_PATTERN = "THREAD_NAME_PATTERN";
  public static final String HARVEST_INTERVAL = "HARVEST_INTERVAL";
  public static final String QUEUE_SIZE = "QUEUE_SIZE";
  public static final String HOSTNAME = "HOSTNAME";

  private EnvironmentVars() {}
}
