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

rootProject.name = "Levyra"
include(":app")

includeBuild("extern/LevyraExtractor") {
    dependencySubstitution {
        substitute(module("com.github.luc4n3x:levyraextractor")).using(project(":extractor"))
    }
}
