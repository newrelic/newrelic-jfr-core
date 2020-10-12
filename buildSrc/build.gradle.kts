plugins {
    id("java")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation ("org.beryx:badass-jlink-plugin:2.22.3")
    implementation ("com.netflix.nebula:gradle-ospackage-plugin:8.4.1")
    implementation (gradleApi())
}