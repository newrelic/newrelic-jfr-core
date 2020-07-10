package com.newrelic.jfr.daemon;

import java.time.Duration;

public class DaemonConfig {

    public static final String DEFAULT_JMX_HOST = "localhost";
    public static final int DEFAULT_JMX_PORT = 1099;
    public static final boolean DEFAULT_USE_SHARED_FILESYSTEM = false;
    private static final Duration DEFAULT_HARVEST_INTERVAL = Duration.ofSeconds(10);

    private final String jmxHost;
    private final Integer jmxPort;
    private final boolean useSharedFilesystem;
    private final Duration harvestInterval;

    public DaemonConfig(Builder builder) {
        this.jmxHost = builder.jmxHost;
        this.jmxPort = builder.jmxPort;
        this.useSharedFilesystem = builder.useSharedFilesystem;
        this.harvestInterval = builder.harvestInterval;
    }

    public String getJmxHost() {
        return jmxHost;
    }

    public Integer getJmxPort() {
        return jmxPort;
    }

    public boolean isUseSharedFilesystem() {
        return useSharedFilesystem;
    }

    public Duration getHarvestInterval() {
        return harvestInterval;
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        private String jmxHost = DEFAULT_JMX_HOST;
        private Integer jmxPort = DEFAULT_JMX_PORT;
        private boolean useSharedFilesystem = DEFAULT_USE_SHARED_FILESYSTEM;
        private Duration harvestInterval = DEFAULT_HARVEST_INTERVAL;

        public Builder jmxHost(String host) {
            this.jmxHost = host;
            return this;
        }

        public Builder jmxPort(int port) {
            this.jmxPort = port;
            return this;
        }

        public Builder useSharedFilesystem(boolean useSharedFilesystem){
            this.useSharedFilesystem = useSharedFilesystem;
            return this;
        }

        public Builder harvestInterval(Duration harvestInterval){
            this.harvestInterval = harvestInterval;
            return this;
        }

        public DaemonConfig build(){
            return new DaemonConfig(this);
        }
    }
}
