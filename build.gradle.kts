
plugins {
    id("com.github.sherter.google-java-format") version "0.9" apply false
}

allprojects {
    group = "com.newrelic.telemetry"
    repositories {
        mavenCentral()
    }
}

val release: String? by project
val junitVersion: String by project
val mockitoVersion: String by project
val newRelicTelemetry: String by project

subprojects {
    version = if("true" == release) "${version}" else "${version}-SNAPSHOT"

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "com.github.sherter.google-java-format")

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
        "testImplementation"("org.mockito:mockito-inline:${mockitoVersion}")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
        disableAutoTargetJvm()
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
                val releasesRepoUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")

                url = if("true" == release) releasesRepoUrl else snapshotsRepoUrl
                credentials {
                    username = System.getenv("SONATYPE_USERNAME")
                    password = System.getenv("SONATYPE_PASSWORD")
                }
            }
        }
    }
}