package com.newrelic.jfr.daemon;

public class DaemonConfig {

    public static final String DEFAULT_JMX_HOST = "localhost";
    public static final int DEFAULT_JMX_PORT = 1099;
    public static final boolean DEFAULT_USE_SHARED_FILESYSTEM = false;

    private final String jmxHost;
    private final Integer jmxPort;
    private final boolean useSharedFilesystem;

    public DaemonConfig(Builder builder) {
        this.jmxHost = builder.jmxHost;
        this.jmxPort = builder.jmxPort;
        this.useSharedFilesystem = builder.useSharedFilesystem;
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

    public static class Builder {
        private  String jmxHost = DEFAULT_JMX_HOST;
        private  Integer jmxPort = DEFAULT_JMX_PORT;
        private  boolean useSharedFilesystem = DEFAULT_USE_SHARED_FILESYSTEM;

        public Builder jmxHost(String host){
            this.jmxHost = host;
            return this;
        }

        public Builder jmxPort(int port){
            this.jmxPort = port;
            return this;
        }
    }
}
