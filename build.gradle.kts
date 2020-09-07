
plugins {
    id("com.github.sherter.google-java-format") version "0.8" apply false
    id("org.beryx.jlink") version("2.22.1") apply false
    id( "org.ysb33r.java.modulehelper") version("0.10.0") apply false
    id("com.github.johnrengelman.shadow") version ("5.2.0") apply false
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
    apply(plugin = "org.ysb33r.java.modulehelper")

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
        "testImplementation"("org.mockito:mockito-junit-jupiter:${mockitoVersion}")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        withJavadocJar()
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
                val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")

                url = if("true" == release) releasesRepoUrl else snapshotsRepoUrl
                credentials {
                    username = System.getenv("SONATYPE_USERNAME")
                    password = System.getenv("SONATYPE_PASSWORD")
                }
            }
        }
    }
}