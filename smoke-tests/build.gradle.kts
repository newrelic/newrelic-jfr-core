import de.undercouch.gradle.tasks.download.Download

plugins {
    id("org.springframework.boot") version "2.4.8"
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
    implementation("org.springframework.boot:spring-boot-starter-web") {
        //snyk issue with no patch option - https://github.com/newrelic/newrelic-jfr-core/pull/196/checks?check_run_id=2687253977
        exclude(group = "org.glassfish", module = "jakarta.el")
    }
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    testImplementation("org.testcontainers:testcontainers:1.15.1")
    constraints {
        testImplementation("org.apache.commons:commons-compress:1.21") {
            because("snyk violations")
        }
    }
    testImplementation("com.squareup.okhttp3:okhttp:3.12.12")
}

tasks.test {
    enabled = false
}

task<Test>("smokeTest") {
    description = "Runs smoke tests."
    group = "verification"

    systemProperty("PROJECT_ROOT_DIR", project.rootDir.toString())
    systemProperty("SMOKE_TESTS_BUILD_LIBS_DIR", project.buildDir.toString() + "/libs")
    systemProperty("JFR_DAEMON_BUILD_LIBS_DIR", project(":jfr-daemon").buildDir.toString() + "/libs")

    useJUnitPlatform()

    dependsOn(tasks.bootJar)
    dependsOn(project(":jfr-daemon").tasks["shadowJar"])

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}
