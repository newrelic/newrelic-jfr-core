package com.newrelic.smoke;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * This class tests the jfr agent extension, defined in the {@code projectDir/jfr-agent-extension}
 * module. To do so, it creates a dummy application and attaches the new relic java agent as an agent,
 * and adds the jfr-agent-extension.jar to the agent extensions directory. Additionally, it creates
 * and a dummy edge application for jfr agent extension to report data to. The tests ensure that
 * jfr agent extension starts running and reports data to the dummy edge application.
 */
class AgentExtensionSmokeTest extends SmokeTestBase {

  private static final Logger logger = LoggerFactory.getLogger(AgentExtensionSmokeTest.class);

  @Test
  void test() throws InterruptedException {
    assertEdgeIsReset();

    var appContainer = buildAppWithAgentExtensionContainer();
    appContainer.start();

    assertEdgeHasEventsAndMetrics(60);
    appContainer.stop();
  }

  private GenericContainer<?> buildAppWithAgentExtensionContainer() {
    var appFilename = "smoke-test-app.jar";

    // Use a dummy license key instead of a valid one. The agent will initialize the jfr agent extension before
    // shutting down due to an invalid license key. This isn't ideal, but is better than requiring a valid license
    // key as a prerequisite to running the test.
    var agentConfig = "-Dnewrelic.config.license_key=DUMMY_LICENSE_KEY "
            + "-Dnewrelic.config.app_name=smoke-test-app-agent-extension "
            + "-Dnewrelic.config.jfr.enabled=true";

    var dockerImage =
        new ImageFromDockerfile()
            .withFileFromPath("smoke-test", Paths.get(SMOKE_TEST_BUILD_LIBS_DIR))
            .withFileFromPath("newrelic", Paths.get(NEW_RELIC_JAVA_AGENT_DIR))
            .withFileFromPath("jfr-agent-extension", Paths.get(JFR_AGENT_EXTENSION_BUILD_LIBS_DIR))
            .withDockerfileFromBuilder(
                builder ->
                    builder
                        .from(JDK_11_IMAGE)
                        .add("smoke-test/smoke-tests-*-SNAPSHOT.jar", appFilename)
                        .add("newrelic/", "newrelic/")
                        .add("jfr-agent-extension/jfr-agent-extension-*-SNAPSHOT.jar", "newrelic/extensions/jfr-agent-extension.jar")
                        .env(jfrEnvVars(Optional.empty()))
                        .entryPoint("java " + agentConfig + " -javaagent:/newrelic/newrelic.jar -jar " + appFilename)
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
