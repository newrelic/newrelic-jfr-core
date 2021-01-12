plugins {
    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
}

configurations {
    testImplementation {
        resolutionStrategy.force("junit:junit:4.13.1")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
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

    useJUnitPlatform()

    dependsOn(tasks.bootJar)

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}
