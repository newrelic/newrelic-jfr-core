val gsonVersion: String by project
val log4jVersion: String by project
val mockitoVersion: String by project
val objenesisVersion: String by project
val slf4jVersion: String by project

plugins {
    id("org.beryx.jlink")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    disableAutoTargetJvm()
}

jpmsExtraModules {
    module("mockito-junit-jupiter-${mockitoVersion}.jar", "mockito.junit.jupiter", mockitoVersion) {
        exports("org.mockito.junit.jupiter")
    }
    module("objenesis-${objenesisVersion}.jar", "org.objenesis", objenesisVersion) {
        exports("org.objenesis")
    }
}

dependencies {
    implementation(project(":jfr-mappers"))
    implementation(jpms.asModule("org.apache.logging.log4j:log4j-core:${log4jVersion}", "org.apache.logging.log4j.core"))
    implementation(jpms.asModule("org.apache.logging.log4j:log4j-api:${log4jVersion}", "log4j.api"))
}

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

application {
    mainClass.set("com.newrelic.jfr.daemon.JFRDaemon")
    mainModule.set("com.newrelic.jfr.daemon")
}

jlink {
    imageDir.set(file("${buildDir}/jlink/${project.name}-${project.version}"))
    imageZip.set(file("${buildDir}/distributions/${project.name}-${project.version}-jlink.zip"))
}

tasks.named("build") {
    dependsOn("jlink")
}
