import de.undercouch.gradle.tasks.download.Download

plugins {
    id("org.springframework.boot") version "2.4.3"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("de.undercouch.download") version "4.1.1"
}

// Main source set compiles against java 8
tasks.withType<JavaCompile>().configureEach {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

// Test source set compiles against java 11
tasks.named<JavaCompile>("compileTestJava") {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })
}

configurations {
    testImplementation {
        resolutionStrategy.force("junit:junit:4.13.1")
    }
    implementation {
        exclude(module = "spring-boot-starter-logging")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    testImplementation("org.testcontainers:testcontainers:1.15.1")
    testImplementation("com.squareup.okhttp3:okhttp:3.12.12")
}

tasks.test {
    enabled = false
}

// The jfr-agent-extension smoke test relies on running an app with the new relic java agent.
// These tasks download and unzip it so that the test can consume it.
task<Download>("downloadNewRelic") {
    src("https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip")
    dest(project.buildDir.toString() + "/newrelic/newrelic-java.zip")
}

task<Copy>("unzipNewRelic") {
    from(zipTree(project.buildDir.toString() + "/newrelic/newrelic-java.zip"))
    into(project.buildDir.toString())
    dependsOn("downloadNewRelic")
}

task<Test>("smokeTest") {
    description = "Runs smoke tests."
    group = "verification"

    systemProperty("PROJECT_ROOT_DIR", project.rootDir.toString())
    systemProperty("SMOKE_TESTS_BUILD_LIBS_DIR", project.buildDir.toString() + "/libs")
    systemProperty("NEW_RELIC_JAVA_AGENT_DIR", project.buildDir.toString() + "/newrelic")
    systemProperty("JFR_DAEMON_BUILD_LIBS_DIR", project(":jfr-daemon").buildDir.toString() + "/libs")
    systemProperty("JFR_AGENT_EXTENSION_BUILD_LIBS_DIR", project(":jfr-agent-extension").buildDir.toString() + "/libs")

    useJUnitPlatform()

    dependsOn(tasks.bootJar)
    dependsOn(project(":jfr-daemon").tasks["shadowJar"])
    dependsOn(project(":jfr-agent-extension").tasks["shadowJar"])
    dependsOn(tasks["unzipNewRelic"])

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}
