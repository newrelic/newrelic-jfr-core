plugins {
    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
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
