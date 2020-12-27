package com.newrelic.smoke;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * This class tests the packaging defined in the {@code projectDir/jfr-jlink} module. To do so, it
 * creates a dummy application for jfr to monitor ({@link this#appContainer}), a dummy edge
 * application for jfr to report data to ({@link this#edgeContainer}), and a docker container with a
 * jfr daemon package installed. The tests ensure that the jfr daemon starts up, connects to the
 * dummy application, and reports data to the dummy edge application.
 */
public class JlinkPackageTest {

  private static final Logger logger = LoggerFactory.getLogger(JlinkPackageTest.class);

  private static String smokeTestsBuildLibsDir;
  private static String jlinkDistributionsDir;
  private static Network network;
  private static GenericContainer<?> edgeContainer;
  private static GenericContainer<?> appContainer;
  private static SmokeTestAppClient edgeContainerClient;

  @BeforeAll
  static void setup() {
    // The gradle smokeTest task sets system properties specifying where to find artifacts needed
    // for testing
    smokeTestsBuildLibsDir = System.getProperty("SMOKE_TESTS_BUILD_LIBS_DIR");
    jlinkDistributionsDir = System.getProperty("JLINK_DISTRIBUTIONS_DIR");

    network = Network.newNetwork();
    edgeContainer = buildAppContainer("smoke-test-edge", false);
    appContainer = buildAppContainer("smoke-test-app", true);

    edgeContainer.start();
    appContainer.start();
    edgeContainerClient = new SmokeTestAppClient("localhost", edgeContainer.getMappedPort(8080));
  }

  @Test
  void debPackageOnDebian() {
    var jfrDebianContainer =
        buildJfrDaemonContainer(
            "debian:stable-slim",
            "apt install /jfr-jlink-distributions/\"$(ls /jfr-jlink-distributions/ | grep '.deb')\"",
            "/usr/lib/jfr-jlink/jfr-daemon-linux-x64/bin/jfr-daemon");
    assertJfrPackage(jfrDebianContainer);
  }

  @Test
  void rpmPackageOnFedora() {
    var jfrFedoraContainer =
        buildJfrDaemonContainer(
            "fedora:34",
            "rpm -i /jfr-jlink-distributions/\"$(ls /jfr-jlink-distributions/ | grep '.rpm')\" --nodigest",
            "/usr/lib/jfr-jlink/jfr-daemon-linux-x64/bin/jfr-daemon");
    assertJfrPackage(jfrFedoraContainer);
  }

  /**
   * Validate that the {@code jfrPackageContainer} is functioning properly.
   *
   * @param jfrPackageContainer the container to test
   */
  private void assertJfrPackage(GenericContainer<?> jfrPackageContainer) {
    // Reset metrics that may have been recorded in other tests
    edgeContainerClient.resetEvents();
    edgeContainerClient.resetMetrics();

    // Register a log consumer and start the container
    var jfrLogConsumer = new WaitingConsumer();
    jfrPackageContainer.withLogConsumer(jfrLogConsumer);
    jfrPackageContainer.start();

    // Wait for log messages indicating data has been reported
    try {
      jfrLogConsumer.waitUntil(
          outputFrame -> outputFrame.getUtf8String().contains("Sending metric batch of size"),
          60,
          TimeUnit.SECONDS);
      jfrLogConsumer.waitUntil(
          outputFrame -> outputFrame.getUtf8String().contains("Sending events batch of size"),
          60,
          TimeUnit.SECONDS);
      Thread.sleep(2000);
    } catch (Exception e) {
      fail(e);
    }

    // Validate the data made it to the edge container
    assertTrue(edgeContainerClient.getEventCount() > 0);
    assertTrue(edgeContainerClient.getMetricCount() > 0);
  }

  /**
   * Build a container for the {@link SmokeTestApp}.
   *
   * @param appHost a friendly host name for the application
   * @param jmxEnabled whether or not to expose jmx
   * @return the application container
   */
  private static GenericContainer<?> buildAppContainer(String appHost, boolean jmxEnabled) {
    var appFilename = "smoke-test-app.jar";

    // Create the entrypoint to run the java app, which optionally exposes jmx
    var preamble = "java -jar ";
    var jvmOpts =
        "-Dcom.sun.management.jmxremote "
            + "-Djava.rmi.server.hostname="
            + appHost
            + " "
            + "-Dcom.sun.management.jmxremote.port=1099 "
            + "-Dcom.sun.management.jmxremote.ssl=false "
            + "-Dcom.sun.management.jmxremote.authenticate=false ";
    var entryPoint = preamble + (jmxEnabled ? jvmOpts : "") + appFilename;

    // Create the docker image definition, equivalent of creating a Dockerfile programmatically
    var dockerImage =
        new ImageFromDockerfile()
            .withFileFromPath("smoke-test", Paths.get(smokeTestsBuildLibsDir))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from("openjdk:11-jre-slim-buster")
                        .add("smoke-test/smoke-tests*.jar", appFilename)
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
   * Build a container to run a jfr package. The container will monitor {@link #appContainer} over
   * jmx, and report data to @link #edgeContainer}.
   *
   * @param dockerImageName the base image name
   * @param installCommand the command to install the jfr package
   * @param entryPoint the entrypoint to start the jfr package
   * @return the container
   */
  private static GenericContainer<?> buildJfrDaemonContainer(
      String dockerImageName, String installCommand, String entryPoint) {
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
            .withFileFromPath("jlink-distributions", Paths.get(jlinkDistributionsDir))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(dockerImageName)
                        // Add the jfr-jlink distributions directory to the image
                        .add("jlink-distributions/jfr-jlink*", "jfr-jlink-distributions/")
                        .run(installCommand)
                        .entryPoint(entryPoint)
                        .env(env)
                        .build());

    // Finally, create the container from the image definition
    return new GenericContainer<>(dockerImage)
        .withNetwork(network)
        .dependsOn(appContainer, edgeContainer)
        // The container is ready when jfr has connected to the application via jmx
        .waitingFor(Wait.forLogMessage("^.*(Connection to remote MBean server complete).*$", 1))
        .withLogConsumer(new Slf4jLogConsumer(logger));
  }
}
