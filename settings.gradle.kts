pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
   }
}

rootProject.name = "newrelic-jfr-core"

include("jfr-mappers")
include("jfr-daemon")
include("jfr-jlink")
