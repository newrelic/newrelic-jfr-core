pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        jcenter()
   }
}

rootProject.name = "newrelic-jfr-core"

include("jfr-daemon")
include("jfr-agent-extension")
include("jfr-jlink")
include("smoke-tests")
include("jfr-mappers")
