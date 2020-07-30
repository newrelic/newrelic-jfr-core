package com.newrelic.jfr.daemon.lifecycle;

import com.newrelic.jfr.daemon.DaemonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

import static javax.management.remote.JMXConnectorFactory.newJMXConnector;

public class MBeanServerConnector {

    private static final Logger logger = LoggerFactory.getLogger(MBeanServerConnector.class);

    private final DaemonConfig config;

    public MBeanServerConnector(DaemonConfig config) {
        this.config = config;
    }

    public MBeanServerConnection getConnection() throws IOException {
        var busyWait = new BusyWait<>("MBeanServerConnection", () -> {
            //        var map = new HashMap<String, Object>();
            //        var credentials = new String[]{"", ""};
            //        map.put("jmx.remote.credentials", credentials);
            var urlPath = "/jndi/rmi://" + config.getJmxHost() + ":" + config.getJmxPort() + "/jmxrmi";
            var url = new JMXServiceURL("rmi", "", 0, urlPath);
            var connector = newJMXConnector(url, null);
            connector.connect();
            MBeanServerConnection result = connector.getMBeanServerConnection();
            logger.info("Connection to remote MBean server complete.");
            return result;
        });
        return busyWait.apply();
    }


}
