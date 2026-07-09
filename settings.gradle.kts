import java.io.File
import java.util.Properties

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

val localProperties = Properties().apply {
    val localPropertiesFile = file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun configuredExtractorPath(): String? {
    return providers.gradleProperty("levyraExtractorPath").orNull
        ?: localProperties.getProperty("levyraExtractorPath")
        ?: System.getenv("LEVYRA_EXTRACTOR_DIR")
}

fun File.normalized(): File {
    return runCatching { canonicalFile }.getOrElse { absoluteFile }
}

fun File.isLevyraExtractorBuild(): Boolean {
    return resolve("settings.gradle").isFile && resolve("extractor/build.gradle").isFile
}

val levyraExtractorBuild = listOfNotNull(
    configuredExtractorPath()?.let { File(it) },
    rootDir.resolve("../LevyraExtractor"),
    rootDir.resolve("extern/LevyraExtractor")
)
    .map { if (it.isAbsolute) it.normalized() else rootDir.resolve(it.path).normalized() }
    .distinctBy { it.path.lowercase() }
    .firstOrNull { it.isLevyraExtractorBuild() }

if (levyraExtractorBuild != null) {
    includeBuild(levyraExtractorBuild.path) {
        dependencySubstitution {
            substitute(module("com.github.luc4n3x:levyraextractor")).using(project(":extractor"))
        }
    }
}
