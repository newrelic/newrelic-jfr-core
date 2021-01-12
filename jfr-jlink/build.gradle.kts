val gsonVersion: String by project
val newRelicTelemetryVersion: String by project
val slf4jVersion: String by project

plugins {
    id("org.beryx.jlink") version("2.22.3")
    id( "org.ysb33r.java.modulehelper") version("0.10.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
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
