package com.newrelic.jfr.daemon.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Backoff;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MBeanConnectionFactoryTest {

  private MBeanConnectionFactory target;

  @BeforeEach
  void setup() {
    target = spy(new MBeanConnectionFactory("localhost", 1099));
  }

  @Test
  void awaitConnection_FailsAfterBackoff() throws Exception {
    var backoff =
        spy(
            Backoff.builder()
                .maxBackoff(10, TimeUnit.MILLISECONDS)
                .backoffFactor(1, TimeUnit.MILLISECONDS)
                .maxRetries(5)
                .build());
    doThrow(new IOException("Error!")).when(target).connect(any());

    assertThrows(Exception.class, () -> target.awaitConnection(backoff));

    verify(backoff, times(6)).nextWaitMs();
    verify(target, times(6)).connect(any());
  }

  @Test
  void awaitConnection_Connects() throws Exception {
    var jmxConnector = mock(JMXConnector.class);
    var connection = mock(MBeanServerConnection.class);

    doReturn(jmxConnector).when(target).connect(any());
    when(jmxConnector.getMBeanServerConnection()).thenReturn(connection);

    assertEquals(connection, target.awaitConnection(Backoff.defaultBackoff()));
    var urlCaptor = ArgumentCaptor.forClass(JMXServiceURL.class);
    verify(target).connect(urlCaptor.capture());
    assertEquals("/jndi/rmi://localhost:1099/jmxrmi", urlCaptor.getValue().getURLPath());
  }
}
