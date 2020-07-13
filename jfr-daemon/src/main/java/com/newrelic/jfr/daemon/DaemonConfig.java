package com.newrelic.jfr.daemon;

import static com.newrelic.jfr.daemon.EnvironmentVars.INSERT_API_KEY;

import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

public class DaemonConfig {

  static final String DEFAULT_JMX_HOST = "localhost";
  static final int DEFAULT_JMX_PORT = 1099;
  static final boolean DEFAULT_USE_SHARED_FILESYSTEM = false;
  static final Duration DEFAULT_HARVEST_INTERVAL = Duration.ofSeconds(10);
  static final String DEFAULT_MONITORED_APP_NAME = "eventing_hobgoblin";

  private final String apiKey;
  private final URI metricsUri;
  private final URI eventsUri;
  private final String jmxHost;
  private final Integer jmxPort;
  private final boolean useSharedFilesystem;
  private final Duration harvestInterval;
  private final String daemonVersion;
  private final String monitoredAppName;

  public DaemonConfig(Builder builder) {
    this.apiKey = builder.apiKey;
    this.metricsUri = builder.metricsUri;
    this.eventsUri = builder.eventsUri;
    this.jmxHost = builder.jmxHost;
    this.jmxPort = builder.jmxPort;
    this.useSharedFilesystem = builder.useSharedFilesystem;
    this.harvestInterval = builder.harvestInterval;
    this.daemonVersion = builder.daemonVersion;
    this.monitoredAppName = builder.monitoredAppName;
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

  public boolean streamFromJmx() {
    return !useSharedFilesystem;
  }

  public Duration getHarvestInterval() {
    return harvestInterval;
  }

  public String getDaemonVersion() {
    return daemonVersion;
  }

  public String getMonitoredAppName() {
    return monitoredAppName;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String apiKey;
    private URI metricsUri;
    private URI eventsUri;
    private String jmxHost = DEFAULT_JMX_HOST;
    private Integer jmxPort = DEFAULT_JMX_PORT;
    private boolean useSharedFilesystem = DEFAULT_USE_SHARED_FILESYSTEM;
    private Duration harvestInterval = DEFAULT_HARVEST_INTERVAL;
    public String daemonVersion = "UNKNOWN-VERSION";
    public String monitoredAppName = DEFAULT_MONITORED_APP_NAME;

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

    public Builder harvestInterval(Duration harvestInterval) {
      this.harvestInterval = harvestInterval;
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
      var envValue = getEnv(envKey);
      if (envValue != null) {
        var value = mapper.apply(envValue);
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
}
