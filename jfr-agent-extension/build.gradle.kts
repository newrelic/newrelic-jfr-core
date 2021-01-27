val gsonVersion: String by project
val newRelicAgentVersion: String by project
val slf4jVersion: String by project

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    api(project(":jfr-daemon"))
    implementation("org.slf4j:slf4j-api:${slf4jVersion}");
    implementation("com.newrelic.agent.java:newrelic-api:${newRelicAgentVersion}")
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes(
                "Premain-Class" to "com.newrelic.jfr.Entrypoint",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "New Relic, Inc."
        )
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.newrelic"
            artifactId = "jfr-agent"
            version = version
            from(components["java"])
            pom {
                name.set(project.name)
                description.set("JFR Agent")
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
