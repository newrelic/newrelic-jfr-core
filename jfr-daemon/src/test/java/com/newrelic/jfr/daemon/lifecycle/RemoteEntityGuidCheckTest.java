package com.newrelic.jfr.daemon.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteEntityGuidCheckTest {

  String guid = "90210";
  AtomicReference<Optional<String>> foundGuid;
  CountDownLatch latch;
  RemoteEntityGuidCheck.LinkingMetadataMBean mockProxy;

  @BeforeEach
  void setup() {
    foundGuid = new AtomicReference<>();
    latch = new CountDownLatch(1);
    mockProxy = mock(RemoteEntityGuidCheck.LinkingMetadataMBean.class);
  }

  @Test
  void testHappyPath_remoteAgentImmediatelyReady() throws Exception {
    var expectedGuid = Optional.of(guid);
    when(mockProxy.readLinkingMetadata()).thenReturn(Map.of("entity.guid", guid));

    RemoteEntityGuidCheck remoteEntityGuidCheck = buildTestClass();

    remoteEntityGuidCheck.start(Duration.ofMillis(1));
    assertTrue(latch.await(1, TimeUnit.SECONDS));
    assertEquals(expectedGuid, foundGuid.get());
  }

  @Test
  void testHappyPath_noRemoteAgent() throws Exception {
    when(mockProxy.readLinkingMetadata()).thenThrow(new RuntimeException("No such thinger"));

    RemoteEntityGuidCheck remoteEntityGuidCheck = buildTestClass();

    remoteEntityGuidCheck.start(Duration.ofMillis(1));
    assertTrue(latch.await(1, TimeUnit.SECONDS));
    assertEquals(Optional.empty(), foundGuid.get());
  }

  @Test
  void testHappyPath_retriesUntilGuidAvailable() throws Exception {
    var expectedGuid = Optional.of(guid);

    when(mockProxy.readLinkingMetadata())
        .thenReturn(Collections.emptyMap())
        .thenReturn(Collections.emptyMap())
        .thenReturn(Map.of("entity.guid", guid));

    RemoteEntityGuidCheck remoteEntityGuidCheck = buildTestClass();

    remoteEntityGuidCheck.start(Duration.ofMillis(1));

    assertTrue(latch.await(5, TimeUnit.SECONDS));
    assertEquals(expectedGuid, foundGuid.get());
  }

  @Test
  public void testVerifyThreadIsShutDown() throws Exception {
    when(mockProxy.readLinkingMetadata()).thenReturn(Map.of("entity.guid", guid));

    var executorService = Executors.newSingleThreadScheduledExecutor();
    RemoteEntityGuidCheck remoteEntityGuidCheck =
        RemoteEntityGuidCheck.builder()
            .mBeanProxyCreator(
                name -> {
                  assertEquals(RemoteEntityGuidCheck.MBEAN_NAME, name.getCanonicalName());
                  return mockProxy;
                })
            .onComplete(
                x -> {
                  latch.countDown();
                })
            .executorService(executorService)
            .build();

    remoteEntityGuidCheck.start(Duration.ofMillis(1));
    assertTrue(latch.await(1, TimeUnit.SECONDS));
    assertTrue(executorService.awaitTermination(1, TimeUnit.SECONDS));
  }

  private RemoteEntityGuidCheck buildTestClass() {
    return RemoteEntityGuidCheck.builder()
        .mBeanProxyCreator(
            name -> {
              assertEquals(RemoteEntityGuidCheck.MBEAN_NAME, name.getCanonicalName());
              return mockProxy;
            })
        .onComplete(
            x -> {
              foundGuid.set(x);
              latch.countDown();
            })
        .build();
  }
}
