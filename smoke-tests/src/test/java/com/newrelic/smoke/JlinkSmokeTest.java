package com.newrelic.smoke;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
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
class JlinkSmokeTest extends SmokeTestBase {

  private static final Logger logger = LoggerFactory.getLogger(JlinkSmokeTest.class);

  @Test
  void test() throws InterruptedException {
    assertEdgeIsReset();

    var appContainer = buildAppContainer();
    var jfrContainer = buildJfrJlinkContainer(appContainer);
    appContainer.start();
    jfrContainer.start();

    assertEdgeHasEventsAndMetrics(60);
    appContainer.stop();
    jfrContainer.stop();
  }

  private GenericContainer<?> buildJfrJlinkContainer(GenericContainer<?> appContainer) {
    var dockerImage =
        new ImageFromDockerfile()
            // Add the whole project to the docker build context
            .withFileFromPath(".", Paths.get(PROJECT_ROOT_DIR))
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
                        .env(jfrEnvVars(Optional.of(appContainer.getNetworkAliases().get(0))))
                        .build());

    return new GenericContainer<>(dockerImage)
        .withNetwork(NETWORK)
        .dependsOn(appContainer, EDGE_CONTAINER)
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .waitingFor(
            Wait.forLogMessage("^.*(BUILD SUCCESSFUL).*$", 1)
                .withStartupTimeout(Duration.ofSeconds(600)));
  }
}
