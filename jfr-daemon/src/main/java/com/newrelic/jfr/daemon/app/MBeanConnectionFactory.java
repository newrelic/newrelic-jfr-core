package com.newrelic.jfr.daemon.app;

import com.newrelic.jfr.daemon.SafeSleep;
import com.newrelic.telemetry.Backoff;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MBeanConnectionFactory {

  private static final Logger logger = LoggerFactory.getLogger(MBeanConnectionFactory.class);

  private final String jmxHost;
  private final int jmxPort;

  public MBeanConnectionFactory(String jmxHost, int jmxPort) {
    this.jmxHost = jmxHost;
    this.jmxPort = jmxPort;
  }

  /**
   * Obtain a connection to the MBean Server, retrying on error according to the backoff policy.
   *
   * @param backoff the backoff policy
   * @return the connection
   * @throws Exception if unable to obtain a connection after backing off
   */
  public MBeanServerConnection awaitConnection(Backoff backoff) throws Exception {
    String urlPath = String.format("/jndi/rmi://%s:%s/jmxrmi", jmxHost, jmxPort);
    JMXServiceURL url = new JMXServiceURL("rmi", "", 0, urlPath);
    while (true) {
      try {
        JMXConnector jmxConnector = connect(url);
        MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();
        logger.info("Connection to remote MBean Sever complete.");
        return connection;
      } catch (IOException e) {
        long backoffMillis = backoff.nextWaitMs();
        if (backoffMillis == -1) {
          throw new Exception(
              "Failed to connect to remote MBean Sever after completing backoff.", e);
        } else {
          logger.info(
              "Error connecting to remote MBean Server. Backing off {} millis.", backoffMillis);
          SafeSleep.sleep(Duration.ofMillis(backoffMillis));
        }
      }
    }
  }

  /**
   * Obtain a backoff instance which will wait indefinitely.
   *
   * @return the backoff instance
   */
  public static Backoff waitForeverBackoff() {
    return Backoff.builder()
        .maxBackoff(15, TimeUnit.SECONDS)
        .backoffFactor(1, TimeUnit.SECONDS)
        .maxRetries(Integer.MAX_VALUE)
        .build();
  }

  JMXConnector connect(JMXServiceURL url) throws IOException {
    return JMXConnectorFactory.connect(url);
  }
}
