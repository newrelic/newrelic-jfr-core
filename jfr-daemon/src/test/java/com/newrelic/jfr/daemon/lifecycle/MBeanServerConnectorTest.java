package com.newrelic.jfr.daemon.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newrelic.jfr.daemon.DaemonConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import org.junit.jupiter.api.Test;

class MBeanServerConnectorTest {

  @Test
  void testGetConnection() throws Exception {
    String host = "some.jmx.example.com";
    int port = 999;
    String expectedServiceUrl = "service:jmx:rmi:///jndi/rmi://some.jmx.example.com:999/jmxrmi";
    DaemonConfig config =
        DaemonConfig.builder().apiKey("sekret").jmxHost(host).jmxPort(port).build();
    AtomicBoolean triedOnce = new AtomicBoolean(false);
    JMXConnector mockConnector = mock(JMXConnector.class);
    MBeanServerConnection expectedConnection = mock(MBeanServerConnection.class);

    when(mockConnector.getMBeanServerConnection()).thenReturn(expectedConnection);

    MBeanServerConnector connector =
        new MBeanServerConnector(config) {
          @Override
          JMXConnector newJMXConnector(JMXServiceURL url) {
            if (triedOnce.compareAndSet(false, true)) {
              throw new RuntimeException(("Ouch, I failed to make a thing."));
            }
            assertEquals(expectedServiceUrl, url.toString());
            return mockConnector;
          }
        };
    MBeanServerConnection result = connector.getConnection(Duration.ofNanos(1));
    verify(mockConnector).connect();
    assertEquals(expectedConnection, result);
    assertTrue(triedOnce.get());
  }
}
