private object Versions {
    const val slf4j = "1.7.26"
    const val gson = "2.8.6"
    const val log4j = "2.13.3"
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
    api("org.apache.logging.log4j:log4j-slf4j-impl:${Versions.log4j}")
    api("org.apache.logging.log4j:log4j-core:${Versions.log4j}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes(
                "Main-Class" to "com.newrelic.jfr.daemon.JFRController",
                "Implementation-Version" to project.version
        )
    }
}

tasks.named("build") { dependsOn("shadowJar") }

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.newrelic"
            artifactId = "jfr-daemon"
            version = version
            from(components["java"])
            pom {
                name.set(project.name)
                description.set("JFR Daemon")
                url.set("https://github.com/newrelic/newrelic-jfr-core")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("newrelic")
                        name.set("New Relic")
                        email.set("opensource@newrelic.com")
                    }
                }
                scm {
                    url.set("git@github.com:newrelic/newrelic-jfr-core.git")
                    connection.set("scm:git:git@github.com:newrelic/newrelic-jfr-core.git")
                }
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingKeyId: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    this.sign(publishing.publications["maven"])
}
