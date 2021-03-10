val newRelicTelemetryVersion: String by project
val gsonVersion: String by project
val slf4jVersion: String by project

dependencies {
    implementation("com.newrelic.telemetry:telemetry-core:${newRelicTelemetryVersion}")
    implementation("com.google.code.gson:gson:${gsonVersion}")
    implementation("org.slf4j:slf4j-simple:${slf4jVersion}");
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

tasks {
    val taskScope = this
    val jar: Jar by taskScope
    jar.apply {
        manifest.attributes["Implementation-Version"] = project.version
        manifest.attributes["Implementation-Vendor"] = "New Relic, Inc"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.newrelic"
            artifactId = "jfr-mappers"
            version = version
            from(components["java"])
            pom {
                name.set(project.name)
                description.set("Library of mappers to turn JFR RecordedEvents into New Relic telemetry")
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
