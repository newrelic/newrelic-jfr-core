package com.newrelic.jfr.daemon.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.newrelic.telemetry.Backoff;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RemoteEntityBridgeTest {

  private MBeanServerConnection connection;

  private RemoteEntityBridge target;

  @BeforeEach
  void setup() {
    connection = mock(MBeanServerConnection.class);

    target = spy(new RemoteEntityBridge());
  }

  @Test
  void fetchRemoteEntityIdAsync() throws ExecutionException, InterruptedException {
    doReturn(Optional.of("1234")).when(target).awaitRemoteEntityId(eq(connection), any());

    assertEquals(Optional.of("1234"), target.fetchRemoteEntityIdAsync(connection).get());
  }

  @Test
  void awaitRemoteEntityGuid_NoRemoteEntity() throws MalformedObjectNameException {
    doThrow(new MalformedObjectNameException("Error!"))
        .when(target)
        .getRemoteEntityGuid(connection);

    assertEquals(
        Optional.empty(), target.awaitRemoteEntityId(connection, Backoff.defaultBackoff()));
  }

  @Test
  void awaitRemoteEntityGuid_RemoteAgentConnect() throws MalformedObjectNameException {
    doReturn(Optional.empty())
        .doReturn(Optional.of("1234"))
        .when(target)
        .getRemoteEntityGuid(connection);
    var backoff = spy(Backoff.defaultBackoff());

    assertEquals(Optional.of("1234"), target.awaitRemoteEntityId(connection, backoff));

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

    assertEquals(Optional.empty(), target.awaitRemoteEntityId(connection, backoff));
    verify(backoff, times(6)).nextWaitMs();
  }
}
