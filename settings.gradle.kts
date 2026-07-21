pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.spotify.ruler") {
                useModule("com.spotify.ruler:ruler-gradle-plugin:${requested.version ?: "2.0.0-beta-3"}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

includeBuild("third_party/LevyraExtractor") {
    dependencySubstitution {
        substitute(module("com.github.LUC4N3X:LevyraExtractor")).using(project(":"))
    }
}

rootProject.name = "Levyra"
include(":app")
include(":baselineprofile")
