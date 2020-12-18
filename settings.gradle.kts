pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
   }
}

rootProject.name = "newrelic-jfr-core"

include("jfr-daemon")
include("jfr-jlink")
include("jfr-mappers")