private object Versions {
    const val slf4j = "1.7.26"
    const val gson = "2.8.6"
    const val logback = "1.2.3"
    const val newRelicTelemetry = "0.6.1"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

apply(plugin = "maven")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    disableAutoTargetJvm()
    withSourcesJar()
}

dependencies {
    api(project(":jfr-mappers"))
    api("com.newrelic.telemetry:telemetry-http-java11:${Versions.newRelicTelemetry}")
    api("org.slf4j:slf4j-api:${Versions.slf4j}")
    api("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
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
