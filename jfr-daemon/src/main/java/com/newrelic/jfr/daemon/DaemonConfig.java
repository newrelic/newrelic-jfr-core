/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.EnvironmentVars.INSERT_API_KEY;

import com.newrelic.jfr.ThreadNameNormalizer;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

public class DaemonConfig {

  static final int DEFAULT_JMX_PORT = 1099;

  private static final String DEFAULT_DAEMON_VERSION = "UNKNOWN-VERSION";
  private static final String DEFAULT_JMX_HOST = "localhost";
  private static final boolean DEFAULT_USE_SHARED_FILESYSTEM = false;
  private static final boolean DEFAULT_USE_LICENSE_KEY = false;
  private static final boolean DEFAULT_AUDIT_LOGGING = false;
  public static final int DEFAULT_HARVEST_INTERVAL = 10;
  private static final Duration DEFAULT_HARVEST_DURATION =
      Duration.ofSeconds(DEFAULT_HARVEST_INTERVAL);
  public static final Integer DEFAULT_QUEUE_SIZE = 250_000;
  private static final String DEFAULT_MONITORED_APP_NAME = "My Application";
  private static final String DEFAULT_PROXY_HOST = null;
  private static final Integer DEFAULT_PROXY_PORT = null;
  private static final String DEFAULT_PROXY_SCHEME = null;
  private static final String DEFAULT_PROXY_USER = null;
  private static final String DEFAULT_PROXY_PASSWORD = null;
  private static final String DEFAULT_HOSTNAME = "localhost";

  private final String apiKey;
  private final URI metricsUri;
  private final URI eventsUri;
  private final String jmxHost;
  private final Integer jmxPort;
  private final boolean useSharedFilesystem;
  private final Duration harvestInterval;
  private final Integer queueSize;
  private final String daemonVersion;
  private final String monitoredAppName;
  private final boolean auditLogging;
  private final boolean useLicenseKey;
  private final String proxyHost;
  private final Integer proxyPort;
  private final String proxyUser;
  private final String proxyPassword;
  private final String proxyScheme;
  private final String threadNamePattern;
  private final String hostname;

  public DaemonConfig(Builder builder) {
    this.auditLogging = builder.auditLogging;
    this.apiKey = builder.apiKey;
    this.metricsUri = builder.metricsUri;
    this.eventsUri = builder.eventsUri;
    this.jmxHost = builder.jmxHost;
    this.jmxPort = builder.jmxPort;
    this.useSharedFilesystem = builder.useSharedFilesystem;
    this.useLicenseKey = builder.useLicenseKey;
    this.harvestInterval = builder.harvestInterval;
    this.queueSize = builder.queueSize;
    this.daemonVersion = builder.daemonVersion;
    this.monitoredAppName = builder.monitoredAppName;
    this.proxyHost = builder.proxyHost;
    this.proxyPort = builder.proxyPort;
    this.proxyUser = builder.proxyUser;
    this.proxyPassword = builder.proxyPassword;
    this.proxyScheme = builder.proxyScheme;
    this.threadNamePattern = builder.threadNamePattern;
    this.hostname = builder.hostname;
  }

  public boolean auditLogging() {
    return auditLogging;
  }

  public String getApiKey() {
    return apiKey;
  }

  public URI getMetricsUri() {
    return metricsUri;
  }

  public URI getEventsUri() {
    return eventsUri;
  }

  public String getJmxHost() {
    return jmxHost;
  }

  public Integer getJmxPort() {
    return jmxPort;
  }

  public boolean useSharedFilesystem() {
    return useSharedFilesystem;
  }

  public boolean useLicenseKey() {
    return useLicenseKey;
  }

  public boolean streamFromJmx() {
    return !useSharedFilesystem;
  }

  public Duration getHarvestInterval() {
    return harvestInterval;
  }

  public Integer getQueueSize() {
    return queueSize;
  }

  public String getDaemonVersion() {
    return daemonVersion;
  }

  public String getMonitoredAppName() {
    return monitoredAppName;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public String getProxyUser() {
    return proxyUser;
  }

  public String getProxyPassword() {
    return proxyPassword;
  }

  public String getProxyScheme() {
    return proxyScheme;
  }

  public String getThreadNamePattern() {
    return threadNamePattern;
  }

  public String getHostname() {
    return hostname;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean auditLogging = DEFAULT_AUDIT_LOGGING;
    private boolean useLicenseKey = DEFAULT_USE_LICENSE_KEY;
    private String apiKey;
    private URI metricsUri;
    private URI eventsUri;
    private String jmxHost = DEFAULT_JMX_HOST;
    private Integer jmxPort = DEFAULT_JMX_PORT;
    private boolean useSharedFilesystem = DEFAULT_USE_SHARED_FILESYSTEM;
    private Duration harvestInterval = DEFAULT_HARVEST_DURATION;
    private Integer queueSize = DEFAULT_QUEUE_SIZE;
    public String daemonVersion = DEFAULT_DAEMON_VERSION;
    public String monitoredAppName = DEFAULT_MONITORED_APP_NAME;
    private String proxyHost = DEFAULT_PROXY_HOST;
    private Integer proxyPort = DEFAULT_PROXY_PORT;
    private String proxyUser = DEFAULT_PROXY_USER;
    private String proxyPassword = DEFAULT_PROXY_PASSWORD;
    private String proxyScheme = DEFAULT_PROXY_SCHEME;
    private String threadNamePattern = ThreadNameNormalizer.DEFAULT_PATTERN;
    private String hostname = DEFAULT_HOSTNAME;

    public Builder auditLogging(boolean auditLogging) {
      this.auditLogging = auditLogging;
      return this;
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder metricsUri(URI metricsUri) {
      this.metricsUri = metricsUri;
      return this;
    }

    public Builder eventsUri(URI eventsUri) {
      this.eventsUri = eventsUri;
      return this;
    }

    public Builder jmxHost(String host) {
      this.jmxHost = host;
      return this;
    }

    public Builder jmxPort(int port) {
      this.jmxPort = port;
      return this;
    }

    public Builder useSharedFilesystem(boolean useSharedFilesystem) {
      this.useSharedFilesystem = useSharedFilesystem;
      return this;
    }

    public Builder useLicenseKey(boolean useLicenseKey) {
      this.useLicenseKey = useLicenseKey;
      return this;
    }

    public Builder harvestInterval(Integer interval) {
      if (interval != null) {
        this.harvestInterval = Duration.ofSeconds(interval);
      }
      return this;
    }

    public Builder queueSize(Integer queueSize) {
      if (queueSize != null) {
        this.queueSize = queueSize;
      }
      return this;
    }

    public Builder daemonVersion(String daemonVersion) {
      this.daemonVersion = daemonVersion;
      return this;
    }

    public Builder monitoredAppName(String monitoredAppName) {
      this.monitoredAppName = monitoredAppName;
      return this;
    }

    public Builder proxyHost(String proxyHost) {
      this.proxyHost = proxyHost;
      return this;
    }

    public Builder proxyPort(Integer proxyPort) {
      this.proxyPort = proxyPort;
      return this;
    }

    public Builder proxyUser(String proxyUser) {
      this.proxyUser = proxyUser;
      return this;
    }

    public Builder proxyPassword(String proxyPassword) {
      this.proxyPassword = proxyPassword;
      return this;
    }

    public Builder proxyScheme(String proxyScheme) {
      this.proxyScheme = proxyScheme;
      return this;
    }

    public Builder threadNamePattern(String threadNamePattern) {
      this.threadNamePattern = threadNamePattern;
      return this;
    }

    public Builder hostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    /**
     * Fetch the given envKey from the environment and, if set, convert it to another type and pass
     * it to the given builder method.
     *
     * @param envKey - The key to look up in the environment
     * @param mapper - A type conversion function
     * @param builderMethod - builder method to invoke
     * @param <T> - generic type of the resulting field in the builder
     * @return the builder object
     */
    public <T> DaemonConfig.Builder maybeEnv(
        String envKey,
        Function<String, T> mapper,
        Function<T, DaemonConfig.Builder> builderMethod) {
      String envValue = getEnv(envKey);
      if (envValue != null) {
        T value = mapper.apply(envValue);
        return builderMethod.apply(value);
      }
      return this;
    }

    // visible for testing
    String getEnv(String envKey) {
      return System.getenv(envKey);
    }

    public DaemonConfig build() {
      if (apiKey == null) {
        throw new RuntimeException(INSERT_API_KEY + " environment variable is required!");
      }
      return new DaemonConfig(this);
    }
  }

  @Override
  public String toString() {
    return "DaemonConfig{"
        + "apiKey='"
        + apiKey
        + '\''
        + ", metricsUri="
        + metricsUri
        + ", eventsUri="
        + eventsUri
        + ", jmxHost='"
        + jmxHost
        + '\''
        + ", jmxPort="
        + jmxPort
        + ", useSharedFilesystem="
        + useSharedFilesystem
        + ", useLicenseKey="
        + useLicenseKey
        + ", harvestInterval="
        + harvestInterval.getSeconds()
        + ", queueSize="
        + queueSize
        + ", daemonVersion='"
        + daemonVersion
        + '\''
        + ", monitoredAppName='"
        + monitoredAppName
        + '\''
        + ", proxyHost='"
        + proxyHost
        + '\''
        + ", proxyPort='"
        + proxyPort
        + '\''
        + ", proxyUser='"
        + proxyUser
        + '\''
        + ", proxyPassword='"
        + proxyPassword
        + '\''
        + ", hostname='"
        + hostname
        + '\''
        + ", proxyScheme='"
        + proxyScheme
        + '\''
        + ", auditLogging="
        + auditLogging
        + '}';
  }
}
