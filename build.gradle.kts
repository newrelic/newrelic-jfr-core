
plugins {
    id("com.github.sherter.google-java-format") version "0.8"
}

private object Versions {
    const val newRelicTelemetry = "0.6.1"
    const val junit = "5.6.2"
    const val mockitoJunit = "3.3.3"
}

allprojects {
    group = "com.newrelic.telemetry"
    repositories {
        mavenCentral()
    }
}

subprojects {

    apply(plugin = "java-library")

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
        "testImplementation"("org.mockito:mockito-junit-jupiter:${Versions.mockitoJunit}")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

}