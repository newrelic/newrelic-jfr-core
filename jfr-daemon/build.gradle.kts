private object Versions {
    const val junit = "5.3.1"
    const val mockito = "2.23.0"
    const val slf4j = "1.7.26"
    const val jsonassert = "1.5.0"
    const val gson = "2.8.6"
    const val logback = "1.2.3"
    const val newRelicTelemetry = "0.6.1"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("gradle.plugin.com.github.sherter.google-java-format:google-java-format-gradle-plugin:0.8")
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

apply(plugin = "com.github.sherter.google-java-format")
apply(plugin = "maven")

group = "com.newrelic.telemetry"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    disableAutoTargetJvm()
    withSourcesJar()
}

dependencies {
    api("com.newrelic.telemetry:telemetry:${Versions.newRelicTelemetry}")
    api("com.newrelic.telemetry:telemetry-http-java11:${Versions.newRelicTelemetry}")
    api("com.newrelic:jfr-mappers:0.1.0-SNAPSHOT")
    api("org.slf4j:slf4j-api:${Versions.slf4j}")
    api("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("com.google.code.gson:gson:${Versions.gson}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testImplementation("org.mockito:mockito-junit-jupiter:3.3.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.shadowJar {
    manifest {
        attributes(
             "Main-Class" to "com.newrelic.jfr.JFRController"   
        )
    }
}

//mainClassName = "com.newrelic.jfr.JFRController"
