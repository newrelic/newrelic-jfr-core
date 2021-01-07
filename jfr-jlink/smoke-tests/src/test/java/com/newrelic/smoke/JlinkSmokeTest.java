package com.newrelic.smoke;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * This class tests the packaging defined in the {@code projectDir/jfr-jlink} module. To do so, it
 * creates a dummy application for jfr to monitor, a dummy edge application for jfr to report data
 * to, and a docker container with a jfr jlink executable running. The tests ensure that jfr jlink
 * executable starts up, connects to the dummy application, and reports data to the dummy edge
 * application.
 */
public class JlinkSmokeTest {

  private static final Logger logger = LoggerFactory.getLogger(JlinkSmokeTest.class);
  private static final String JDK_11_IMAGE = "adoptopenjdk:11";

  private String smokeTestsBuildLibsDir;
  private String projectRootDir;
  private Network network;

  @BeforeEach
  void setup() {
    // The gradle smokeTest task sets system properties specifying where to find artifacts needed
    // for testing
    smokeTestsBuildLibsDir = System.getProperty("SMOKE_TESTS_BUILD_LIBS_DIR");
    projectRootDir = System.getProperty("PROJECT_ROOT_DIR");
    network = Network.newNetwork();
  }

  @Test
  void test() throws InterruptedException {
    // Setup the edge, the app, and the jfr containers
    var edgeContainer = buildAppContainer("smoke-test-edge", false);
    var appContainer = buildAppContainer("smoke-test-app", true);
    edgeContainer.start();
    appContainer.start();
    var jfrContainer = buildJfrDaemonContainer(edgeContainer, appContainer);
    jfrContainer.start();

    // Wait until the edge has received events and metrics
    var edgeContainerClient =
        new SmokeTestAppClient("localhost", edgeContainer.getMappedPort(8080));
    boolean edgeReceivedEvents = false;
    boolean edgeReceivedMetrics = false;
    int maxSeconds = 60;
    for (int i = 0; i < maxSeconds; i++) {
      if (!edgeReceivedEvents) {
        edgeReceivedEvents = edgeContainerClient.getEventCount() > 0;
      }
      if (!edgeReceivedMetrics) {
        edgeReceivedMetrics = edgeContainerClient.getMetricCount() > 0;
      }
      if (edgeReceivedEvents && edgeReceivedMetrics) {
        break;
      } else {
        logger.info("Edge has not received events after waiting {} seconds.", i);
        Thread.sleep(1000);
      }
    }
    assertTrue(edgeReceivedEvents, "Edge has not received events after " + maxSeconds + " seconds");
    assertTrue(
        edgeReceivedMetrics, "Edge has not received metrics after " + maxSeconds + " seconds");
  }

  /**
   * Build a container for the {@link SmokeTestApp}.
   *
   * @param appHost a friendly host name for the application
   * @param jmxEnabled whether or not to expose jmx
   * @return the application container
   */
  private GenericContainer<?> buildAppContainer(String appHost, boolean jmxEnabled) {
    var appFilename = "smoke-test-app.jar";

    // Create the entrypoint to run the java app, which optionally exposes jmx
    var preamble = "java -jar ";
    var jmxOpts =
        "-Dcom.sun.management.jmxremote "
            + "-Djava.rmi.server.hostname="
            + appHost
            + " "
            + "-Dcom.sun.management.jmxremote.port=1099 "
            + "-Dcom.sun.management.jmxremote.ssl=false "
            + "-Dcom.sun.management.jmxremote.authenticate=false ";
    var entryPoint = preamble + (jmxEnabled ? jmxOpts : "") + appFilename;

    // Create the docker image definition, equivalent of creating a Dockerfile programmatically
    var dockerImage =
        new ImageFromDockerfile()
            .withFileFromPath("smoke-test", Paths.get(smokeTestsBuildLibsDir))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(JDK_11_IMAGE)
                        .add("smoke-test/smoke-tests-*-SNAPSHOT.jar", appFilename)
                        .entryPoint(entryPoint)
                        .build());

    // Finally, create the container from the image definition
    return new GenericContainer<>(dockerImage)
        .withNetwork(network)
        .withNetworkAliases(appHost)
        .withExposedPorts(jmxEnabled ? new Integer[] {8080, 1099} : new Integer[] {8080})
        // The container is ready when GET /ping returns a 2XX response
        .waitingFor(Wait.forHttp("/ping").forPort(8080))
        .withLogConsumer(new Slf4jLogConsumer(logger));
  }

  /**
   * Build a container to run a jfr package. The container will monitor {@code appContainer} over
   * jmx, and report data to {@code edgeContainer}.
   *
   * @param edgeContainer the edge container
   * @param appContainer the edge container
   * @return the container
   */
  private GenericContainer<?> buildJfrDaemonContainer(
      GenericContainer<?> edgeContainer, GenericContainer<?> appContainer) {
    // Define the environment variables required for jfr to connect an app and report data
    var edgeHost = edgeContainer.getNetworkAliases().get(0);
    var appHost = appContainer.getNetworkAliases().get(0);
    var env = new HashMap<String, String>();
    env.put("INSIGHTS_INSERT_KEY", "fake-api-key");
    env.put("REMOTE_JMX_HOST", appHost);
    env.put("EVENTS_INGEST_URI", String.format("http://%s:8080/event/add", edgeHost));
    env.put("METRICS_INGEST_URI", String.format("http://%s:8080/metric/add", edgeHost));

    // Create the docker image definition, equivalent of creating a Dockerfile programmatically
    var dockerImage =
        new ImageFromDockerfile()
            // Add the whole project to the docker build context
            .withFileFromPath(".", Paths.get(projectRootDir))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(JDK_11_IMAGE)
                        // Copy the whole project into the image allowing the jlink executable to be
                        // built inside the container
                        .add("/", "newrelic-jfr-core/")
                        // Build the jlink executable inside the container (ensuring its compatible
                        // with the system architecture) and run it
                        .entryPoint(
                            "/bin/bash",
                            "-c",
                            "cd /newrelic-jfr-core && ./gradlew jlink && $(find /newrelic-jfr-core/jfr-jlink/build/jlink -type f -name jfr-daemon)")
                        .env(env)
                        .build());

    // Finally, create the container from the image definition
    return new GenericContainer<>(dockerImage)
        .withNetwork(network)
        .dependsOn(appContainer, edgeContainer)
        .waitingFor(
            Wait.forLogMessage("^.*(BUILD SUCCESSFUL).*$", 1)
                .withStartupTimeout(Duration.ofSeconds(600)));
  }
}
