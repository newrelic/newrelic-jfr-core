val gsonVersion: String by project
val newRelicTelemetryVersion: String by project
val okhttpVersion: String by project
val slf4jVersion: String by project
val newRelicAgentApiVersion: String by project

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
    api("com.newrelic.telemetry:telemetry-core:${newRelicTelemetryVersion}")
    implementation("com.squareup.okhttp3:okhttp:${okhttpVersion}")
    implementation("com.google.code.gson:gson:${gsonVersion}")
    /*
     * Only require the newrelic-api to compile but do not include the classes in the jfr-daemon jar.
     * The newrelic-api will be provided on the classpath when the jfr-daemon jar is used in the agent.
     * In other jfr-daemon use cases the newrelic-api shouldn't be necessary.
     */
    compileOnly("com.newrelic.agent.java:newrelic-api:${newRelicAgentApiVersion}")

    // Provide the newrelic-api on the runtime classpath for tests.
    testRuntimeOnly("com.newrelic.agent.java:newrelic-api:${newRelicAgentApiVersion}")
}

tasks.jar {
// Create shadowJar instead of jar
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("")

    manifest {
        attributes(
                "Agent-Class" to "com.newrelic.jfr.daemon.agent.AgentMain",
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
            artifact(tasks.javadocJar)
            artifact(tasks.sourcesJar)
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

