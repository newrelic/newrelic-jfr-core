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
 * This class tests the jfr daemon, defined in the {@code projectDir/jfr-daemon} module, as a
 * javaagent. To do so, it creates a dummy application and attaches the jfr daemon as an agent and a
 * dummy edge application for jfr to report data to. The tests ensure that jfr daemon agent starts
 * running and reports data to the dummy edge application.
 */
class AgentSmokeTest extends SmokeTestBase {

  private static final Logger logger = LoggerFactory.getLogger(AgentSmokeTest.class);

  @Test
  void test() throws InterruptedException {
    assertEdgeIsReset();

    var appContainer = buildAppWithAgentContainer();
    appContainer.start();

    assertEdgeHasEventsAndMetrics(60);
    appContainer.stop();
  }

  private GenericContainer<?> buildAppWithAgentContainer() {
    var appFilename = "smoke-test-app.jar";
    var jfrDaemonFilename = "jfr-daemon.jar";

    var dockerImage =
        new ImageFromDockerfile()
            .withFileFromPath("smoke-test", Paths.get(SMOKE_TEST_BUILD_LIBS_DIR))
            .withFileFromPath("jfr-daemon", Paths.get(JFR_DAEMON_BUILD_LIBS_DIR))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(JDK_11_IMAGE)
                        .add("smoke-test/smoke-tests-*-SNAPSHOT.jar", appFilename)
                        .add("jfr-daemon/jfr-daemon-*-SNAPSHOT.jar", jfrDaemonFilename)
                        .env(jfrEnvVars(Optional.empty()))
                        .entryPoint("java -javaagent:" + jfrDaemonFilename + " -jar " + appFilename)
                        .build());

    return new GenericContainer<>(dockerImage)
        .withNetwork(NETWORK)
        .withNetworkAliases("smoke-test-app")
        .dependsOn(EDGE_CONTAINER)
        .withExposedPorts(APP_PORT)
        .waitingFor(APP_IS_READY)
        .withLogConsumer(new Slf4jLogConsumer(logger));
  }
}
