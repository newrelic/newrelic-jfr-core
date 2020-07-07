private object Versions {
    const val slf4j = "1.7.26"
    const val gson = "2.8.6"
    const val logback = "1.2.3"
}

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    disableAutoTargetJvm()
}

dependencies {
    api(project(":jfr-mappers"))
    api("org.slf4j:slf4j-api:${Versions.slf4j}")
    api("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
}

tasks.shadowJar {
    manifest {
        attributes(
             "Main-Class" to "com.newrelic.jfr.daemon.JFRController"
        )
    }
}

//mainClassName = "com.newrelic.jfr.JFRController"
