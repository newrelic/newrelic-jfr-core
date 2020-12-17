val gsonVersion: String by project
val log4jVersion: String by project
//val mockitoVersion: String by project
val newRelicTelemetryVersion: String by project
//val objenesisVersion: String by project
val slf4jVersion: String by project

plugins {
    id("org.beryx.jlink")
    id("com.newrelic.jfr.package")
    id("nebula.ospackage")
    id("org.ysb33r.java.modulehelper")
}

sourceSets {
    main {
        java.setSrcDirs(listOf(
                "src/main/java",
                ".generated-src/java"
        ))
        resources.setSrcDirs(listOf(
                "src/main/resources",
                ".generated-src/resources"
        ))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    disableAutoTargetJvm()
}

jpmsExtraModules {
    module("gson-${gsonVersion}.jar", "com.google.code.gson", gsonVersion) {
        exports("com.google.gson")
    }

}
dependencies {
    implementation("com.google.code.gson:gson:${gsonVersion}")
    implementation("com.newrelic.telemetry:telemetry-all:${newRelicTelemetryVersion}")
    implementation("org.slf4j:slf4j-simple:${slf4jVersion}")
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

    launcher {
        name = "jfr-daemon"
    }
}

tasks.register<Copy>("copySources") {
    group = "Build"
    description = "Copies sources from other subprojects"
    into(".generated-src")
    from("../jfr-daemon/src/main/java") {
        into("java")
    }
    from("../jfr-daemon/src/main/resources") {
        into("resources")
    }
    from("../jfr-mappers/src/main/java") {
        into("java")
    }
    from("../jfr-mappers/src/main/resources") {
        into("resources")
    }
}

tasks.named("compileJava") {
    dependsOn("copySources")
}

tasks.named("processResources") {
    dependsOn("copySources")
}

tasks.named<Delete>("clean") {
    delete(".generated-src")
}

tasks.named("build") {
    dependsOn("jlink")
}
