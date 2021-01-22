package com.newrelic.jfr.daemon.app;

import static com.newrelic.jfr.daemon.AttributeNames.APP_NAME;
import static com.newrelic.jfr.daemon.AttributeNames.ENTITY_GUID;
import static com.newrelic.jfr.daemon.AttributeNames.SERVICE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.Backoff;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RemoteEntityUtilsTest {

  private final String appName = "dummy app";

  private Attributes attributes;
  private MBeanServerConnection connection;

  private RemoteEntityBridge target;

  @BeforeEach
  void setup() throws Exception {
    attributes = new Attributes();
    connection = mock(MBeanServerConnection.class);

    var connectionFactory = mock(MBeanConnectionFactory.class);
    when(connectionFactory.awaitConnection(any())).thenReturn(connection);

    target = spy(new RemoteEntityBridge(connectionFactory));
  }

  @Test
  void connectAndAppendRemoteAttributes_HasEntity() throws Exception {
    doReturn(Optional.of("1234")).when(target).awaitRemoteEntityGuid(eq(connection), any());

    target.connectAndAppendRemoteAttributes(attributes, appName);

    assertEquals("1234", attributes.asMap().get(ENTITY_GUID));
  }

  @Test
  void connectAndAppendRemoteAttributes_NoEntity() throws Exception {
    doReturn(Optional.empty()).when(target).awaitRemoteEntityGuid(eq(connection), any());

    target.connectAndAppendRemoteAttributes(attributes, appName);

    assertEquals(appName, attributes.asMap().get(APP_NAME));
    assertEquals(appName, attributes.asMap().get(SERVICE_NAME));
  }

  @Test
  void awaitRemoteEntityGuid_NoRemoteEntity() throws MalformedObjectNameException {
    doThrow(new MalformedObjectNameException("Error!"))
        .when(target)
        .getRemoteEntityGuid(connection);

    assertEquals(
        Optional.empty(), target.awaitRemoteEntityGuid(connection, Backoff.defaultBackoff()));
  }

  @Test
  void awaitRemoteEntityGuid_RemoteAgentConnect() throws MalformedObjectNameException {
    doReturn(Optional.empty())
        .doReturn(Optional.of("1234"))
        .when(target)
        .getRemoteEntityGuid(connection);
    var backoff = spy(Backoff.defaultBackoff());

    assertEquals(Optional.of("1234"), target.awaitRemoteEntityGuid(connection, backoff));

    verify(backoff).nextWaitMs();
  }

  @Test
  void awaitRemoteEntityGuid_RemoteAgentBackoffLimit() throws MalformedObjectNameException {
    doReturn(Optional.empty()).when(target).getRemoteEntityGuid(connection);
    var backoff =
        spy(
            Backoff.builder()
                .maxBackoff(5, TimeUnit.MILLISECONDS)
                .backoffFactor(1, TimeUnit.MILLISECONDS)
                .maxRetries(5)
                .build());

    assertEquals(Optional.empty(), target.awaitRemoteEntityGuid(connection, backoff));
    verify(backoff, times(6)).nextWaitMs();
  }
}
