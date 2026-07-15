import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licensee)
    alias(libs.plugins.ruler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun envOrProperty(name: String, propertyName: String = name): String {
    val gradleProperty = findProperty(propertyName) as? String
    val localProperty = if (localProperties.containsKey(propertyName)) {
        localProperties.getProperty(propertyName)
    } else {
        null
    }
    val environmentValue = System.getenv(name)
    return (gradleProperty ?: localProperty ?: environmentValue ?: "").trim()
}

fun buildConfigString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    return "\"$escaped\""
}

fun isReleaseTaskRequested(): Boolean {
    return gradle.startParameter.taskNames.any { task ->
        task.contains("Release", ignoreCase = true) ||
            task == "bundle" ||
