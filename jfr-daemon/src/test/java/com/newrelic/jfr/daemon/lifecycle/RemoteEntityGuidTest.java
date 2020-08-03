package com.newrelic.jfr.daemon.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

class RemoteEntityGuidTest {

  @Test
  void testHappyPath_remoteAgent() {
    String guid = "90210";
    MBeanServerConnection connection = mock(MBeanServerConnection.class);
    RemoteEntityGuid.LinkingMetadataMBean mockProxy =
        mock(RemoteEntityGuid.LinkingMetadataMBean.class);

    when(mockProxy.readLinkingMetadata()).thenReturn(Map.of("entity.guid", guid));

    RemoteEntityGuid remoteEntityGuid =
        new RemoteEntityGuid(connection) {
          @Override
          LinkingMetadataMBean newMBeanProxy(ObjectName name) {
            assertEquals(RemoteEntityGuid.MBEAN_NAME, name.getCanonicalName());
            return mockProxy;
          }
        };

    Optional<String> result = remoteEntityGuid.queryFromJmx();
    assertEquals(guid, result.get());
  }

  @Test
  void testHappyPath_noRemoteAgent() {
    MBeanServerConnection connection = mock(MBeanServerConnection.class);
    RemoteEntityGuid.LinkingMetadataMBean mockProxy =
        mock(RemoteEntityGuid.LinkingMetadataMBean.class);

    when(mockProxy.readLinkingMetadata()).thenThrow(new RuntimeException("No such thinger"));

    RemoteEntityGuid remoteEntityGuid =
        new RemoteEntityGuid(connection) {
          @Override
          LinkingMetadataMBean newMBeanProxy(ObjectName name) {
            assertEquals(RemoteEntityGuid.MBEAN_NAME, name.getCanonicalName());
            return mockProxy;
          }
        };

    Optional<String> result = remoteEntityGuid.queryFromJmx();
    assertTrue(result.isEmpty());
  }

  @Test
  void testHappyPath_retriesUntilGuidAvailable() {
    String guid = "abc123";
    MBeanServerConnection connection = mock(MBeanServerConnection.class);
    RemoteEntityGuid.LinkingMetadataMBean mockProxy =
        mock(RemoteEntityGuid.LinkingMetadataMBean.class);

    when(mockProxy.readLinkingMetadata())
        .thenReturn(Collections.emptyMap())
        .thenReturn(Collections.emptyMap())
        .thenReturn(Map.of("entity.guid", guid));

    RemoteEntityGuid remoteEntityGuid =
        new RemoteEntityGuid(connection) {
          @Override
          LinkingMetadataMBean newMBeanProxy(ObjectName name) {
            assertEquals(RemoteEntityGuid.MBEAN_NAME, name.getCanonicalName());
            return mockProxy;
          }
        };

    Optional<String> result = remoteEntityGuid.queryFromJmx(Duration.ofNanos(1));
    assertEquals(guid, result.get());
  }
}
