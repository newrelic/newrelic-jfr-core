package com.newrelic.smoke;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

public abstract class SmokeTestBase {

  private static final Logger logger = LoggerFactory.getLogger(SmokeTestBase.class);

  static final String JDK_11_IMAGE = "adoptopenjdk:11";
  static final int APP_PORT = 8080;
  static final int APP_JMX_PORT = 1099;
  static final WaitStrategy APP_IS_READY = Wait.forHttp("/ping").forPort(APP_PORT);

  static String PROJECT_ROOT_DIR;
  static String SMOKE_TEST_BUILD_LIBS_DIR;
  static String NEW_RELIC_JAVA_AGENT_DIR;
  static String JFR_DAEMON_BUILD_LIBS_DIR;
  static String JFR_AGENT_EXTENSION_BUILD_LIBS_DIR;

  static Network NETWORK;
  static GenericContainer<?> EDGE_CONTAINER;
  static SmokeTestAppClient EDGE_CLIENT;

  List<GenericContainer<?>> containersToStop;

  @BeforeAll
  static void setupNetworkAndEdge() {
    PROJECT_ROOT_DIR = System.getProperty("PROJECT_ROOT_DIR");
    SMOKE_TEST_BUILD_LIBS_DIR = System.getProperty("SMOKE_TESTS_BUILD_LIBS_DIR");
    NEW_RELIC_JAVA_AGENT_DIR = System.getProperty("NEW_RELIC_JAVA_AGENT_DIR");
    JFR_DAEMON_BUILD_LIBS_DIR = System.getProperty("JFR_DAEMON_BUILD_LIBS_DIR");
    JFR_AGENT_EXTENSION_BUILD_LIBS_DIR = System.getProperty("JFR_AGENT_EXTENSION_BUILD_LIBS_DIR");

    NETWORK = Network.newNetwork();
    EDGE_CONTAINER = buildEdgeContainer();
    EDGE_CONTAINER.start();
    EDGE_CLIENT = new SmokeTestAppClient("localhost", EDGE_CONTAINER.getMappedPort(APP_PORT));
  }

  @BeforeEach
  void setup() {
    containersToStop = new ArrayList<>();
    EDGE_CLIENT.resetEvents();
    EDGE_CLIENT.resetMetrics();
  }

  @AfterEach
  void stopContainers() {
    containersToStop.forEach(
        container -> {
          try {
            container.stop();
          } catch (Exception e) {
            logger.info("An error occurred stopping container.");
          }
        });
  }

  private static GenericContainer<?> buildEdgeContainer() {
    var appFilename = "smoke-test-app.jar";

    var dockerImage =
        new ImageFromDockerfile()
            .withFileFromPath("smoke-test", Paths.get(SMOKE_TEST_BUILD_LIBS_DIR))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(JDK_11_IMAGE)
                        .add("smoke-test/smoke-tests-*-SNAPSHOT.jar", appFilename)
                        .entryPoint("java -jar " + appFilename));

    return new GenericContainer<>(dockerImage)
        .withNetwork(NETWORK)
        .withNetworkAliases("smoke-test-edge")
        .withExposedPorts(APP_PORT)
        .waitingFor(APP_IS_READY)
        .withLogConsumer(new Slf4jLogConsumer(logger));
  }

  void cleanupContainer(GenericContainer<?> container) {
    containersToStop.add(container);
  }

  GenericContainer<?> buildAppContainer() {
    var appHost = "smoke-test-app";
    var appFilename = "smoke-test-app.jar";

    var dockerImage =
        new ImageFromDockerfile()
            .withFileFromPath("smoke-test", Paths.get(SMOKE_TEST_BUILD_LIBS_DIR))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(JDK_11_IMAGE)
                        .add("smoke-test/smoke-tests-*-SNAPSHOT.jar", appFilename)
                        .entryPoint("java " + jmxOptions(appHost) + " -jar " + appFilename));

    return new GenericContainer<>(dockerImage)
        .withNetwork(NETWORK)
        .withNetworkAliases("smoke-test-app")
        .withExposedPorts(APP_PORT, APP_JMX_PORT)
        .waitingFor(APP_IS_READY)
        .withLogConsumer(new Slf4jLogConsumer(logger));
  }

  void assertEdgeHasEventsAndMetrics(int maxSecondsToWait) throws InterruptedException {
    boolean hasEvents = false;
    boolean hasMetrics = false;
    for (int i = 0; i < maxSecondsToWait; i++) {
      if (!hasEvents) {
        hasEvents = EDGE_CLIENT.getEventCount() > 0;
      }
      if (!hasMetrics) {
        hasMetrics = EDGE_CLIENT.getMetricCount() > 0;
      }
      if (hasEvents && hasMetrics) {
        break;
      } else {
        logger.info("Edge has not received events or metrics after waiting {} seconds.", i);
        Thread.sleep(1000);
      }
    }
    assertTrue(hasEvents, "Edge has not received events after " + maxSecondsToWait + " seconds");
    assertTrue(hasMetrics, "Edge has not received metrics after " + maxSecondsToWait + " seconds");
  }

  void assertEdgeIsReset() {
    assertEquals(0, EDGE_CLIENT.getEventCount());
    assertEquals(0, EDGE_CLIENT.getMetricCount());
  }

  String jmxOptions(String appHost) {
    return "-Dcom.sun.management.jmxremote "
        + "-Djava.rmi.server.hostname="
        + appHost
        + " "
        + "-Dcom.sun.management.jmxremote.port="
        + APP_JMX_PORT
        + " "
        + "-Dcom.sun.management.jmxremote.ssl=false "
        + "-Dcom.sun.management.jmxremote.authenticate=false ";
  }

  Map<String, String> jfrEnvVars(Optional<String> optJmxHost) {
    String edgeHost = EDGE_CONTAINER.getNetworkAliases().get(0);
    var env = new HashMap<String, String>();
    env.put("INSIGHTS_INSERT_KEY", "fake-api-key");
    env.put("EVENTS_INGEST_URI", String.format("http://%s:%s/event/add", edgeHost, APP_PORT));
    env.put("METRICS_INGEST_URI", String.format("http://%s:%s/metric/add", edgeHost, APP_PORT));
    optJmxHost.ifPresent(jmxHost -> env.put("REMOTE_JMX_HOST", jmxHost));
    return env;
  }
}
