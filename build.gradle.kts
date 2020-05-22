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
}

plugins {
    id("java-library")
    `maven-publish`
}

group = "com.newrelic"

apply(plugin = "com.github.sherter.google-java-format")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    disableAutoTargetJvm()
    withSourcesJar()
}

dependencies {
    api("com.newrelic.telemetry:telemetry:0.5.1")

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

tasks {
    val taskScope = this
    val jar: Jar by taskScope
    jar.apply {
        manifest.attributes["Implementation-Version"] = project.version
        manifest.attributes["Implementation-Vendor"] = "New Relic, Inc"
    }
}

publishing {
    repositories {
        maven {
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("SONATYPE_USERNAME")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.newrelic"
            artifactId = "jfr-mappers"
            version = version
        }
    }
}
