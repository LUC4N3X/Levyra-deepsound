pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
}

includeBuild("../third_party/LevyraExtractor") {
    dependencySubstitution {
        substitute(module("com.github.LUC4N3X:LevyraExtractor")).using(project(":"))
    }
}

includeBuild("../third_party/LevyraNexus") {
    dependencySubstitution {
        substitute(module("com.github.LUC4N3X:LevyraNexus")).using(project(":"))
    }
}

rootProject.name = "levyra-desktop"

include(":core")
include(":player")
include(":app")
