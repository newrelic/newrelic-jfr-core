package com.newrelic.jfr.daemon;

public class EnvironmentVars {

  public static final String INSERT_API_KEY = "INSIGHTS_INSERT_KEY";
  public static final String METRICS_INGEST_URI = "METRICS_INGEST_URI";
  public static final String EVENTS_INGEST_URI = "EVENTS_INGEST_URI";
  public static final String ENV_APP_NAME = "NEW_RELIC_APP_NAME";

  private EnvironmentVars() {}
}
