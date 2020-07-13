
plugins {
    id("com.github.sherter.google-java-format") version "0.8" apply false
}

allprojects {
    group = "com.newrelic.telemetry"
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

object Versions {
    const val junit = "5.6.2"
    const val mockitoJunit = "3.3.3"
    const val newRelicTelemetry = "0.6.1"
}

subprojects {

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "com.github.sherter.google-java-format")

    dependencies {
        "api"("com.newrelic.telemetry:telemetry:${Versions.newRelicTelemetry}")
        "api"("com.newrelic.telemetry:telemetry-http-java11:${Versions.newRelicTelemetry}")
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
        "testImplementation"("org.mockito:mockito-junit-jupiter:${Versions.mockitoJunit}")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.named<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
    }

    configure<PublishingExtension> {
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
    }
}