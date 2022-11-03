val gsonVersion: String by project

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    api(project(":jfr-daemon"))
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes(
                "Main-Class" to "com.newrelic.jfr.tools.StatsMaker",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "New Relic, Inc."
        )
    }
    // Ensure module-info.class files from dependencies don't erroneously make it into the jar
    exclude("**/module-info.class")
    exclude("module-info.class")
}

tasks.named("build") {
    dependsOn("shadowJar")
}
