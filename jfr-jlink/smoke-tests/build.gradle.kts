plugins {
    id("org.springframework.boot") version "2.4.0"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
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

    useJUnitPlatform()

    dependsOn(":jfr-jlink:package")
    dependsOn(tasks.bootJar)

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
}
