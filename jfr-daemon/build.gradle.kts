val gsonVersion: String by project
val newRelicTelemetryVersion: String by project
val okhttpVersion: String by project
val slf4jVersion: String by project

plugins {
    id("com.github.johnrengelman.shadow") version ("5.2.0")
}

// Main source set compiles against java 8
tasks.withType<JavaCompile>().configureEach {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

// Test source set compiles against java 11
tasks.named<JavaCompile>("compileTestJava") {
    javaCompiler.set(javaToolchains.compilerFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })
}

dependencies {
    api(project(":jfr-mappers"))
    implementation("org.slf4j:slf4j-simple:${slf4jVersion}");
    implementation("com.newrelic.telemetry:telemetry-core:0.11.0")
    implementation("com.squareup.okhttp3:okhttp:${okhttpVersion}")
    implementation("com.google.code.gson:gson:${gsonVersion}")
}

tasks.jar {
    // Create shadowJar instead of jar
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")

    manifest {
        attributes(
                // Agent-Class ?
                "Premain-Class" to "com.newrelic.jfr.daemon.agent.AgentMain",
                "Main-Class" to "com.newrelic.jfr.daemon.app.JFRDaemon",
                "Implementation-Version" to project.version
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
            artifactId = "jfr-daemon"
            version = version
            project.shadow.component(this)
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


