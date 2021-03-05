package com.newrelic.smoke;

import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * This class tests the jfr daemon, defined in the {@code projectDir/jfr-daemon} module, as a stand
 * alone java process. To do so, it creates a dummy application for jfr to monitor, a dummy edge
 * application for jfr to report data to, and a docker container with a jfr daemon process running.
 * The tests ensure that jfr daemon process executable starts up, connects to the dummy application,
 * and reports data to the dummy edge application.
 */
class DaemonAppSmokeTest extends SmokeTestBase {

  private static final Logger logger = LoggerFactory.getLogger(DaemonAppSmokeTest.class);

  @Test
  void test() throws InterruptedException {
    assertEdgeIsReset();

    var appContainer = buildAppContainer();
    cleanupContainer(appContainer);
    var jfrContainer = buildDaemonContainer(appContainer);
    cleanupContainer(jfrContainer);
    appContainer.start();
    jfrContainer.start();

    assertEdgeHasEventsAndMetrics(60);
  }

  private GenericContainer<?> buildDaemonContainer(GenericContainer<?> appContainer) {
    var jfrDaemonFilename = "jfr-daemon.jar";

    var dockerImage =
        new ImageFromDockerfile()
            .withFileFromPath("jfr-daemon", Paths.get(JFR_DAEMON_BUILD_LIBS_DIR))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(JDK_8_IMAGE)
                        .add("jfr-daemon/jfr-daemon-*-SNAPSHOT.jar", jfrDaemonFilename)
                        .entryPoint("java -jar " + jfrDaemonFilename)
                        .env(jfrEnvVars(Optional.of(appContainer.getNetworkAliases().get(0))))
                        .build());

    return new GenericContainer<>(dockerImage)
        .withNetwork(NETWORK)
        .dependsOn(appContainer, EDGE_CONTAINER)
        .withLogConsumer(new Slf4jLogConsumer(logger));
  }
}
