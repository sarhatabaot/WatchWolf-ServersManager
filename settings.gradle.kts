rootProject.name = "WatchWolf-ServersManager"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.gradleup.shadow") {
                useModule("com.gradleup.shadow:shadow-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
