import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

val repositoryProperties = Properties().apply {
    val propertiesFile = rootProject.file("../gradle.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use(::load)
    }
}

val levyraDesktopVersion = providers.gradleProperty("levyraDesktopVersion").getOrElse(
    repositoryProperties.getProperty("levyraVersionName") ?: "1.0.0"
)

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("../app/src/main/java/com/luc4n3x/levyra/ui/i18n"))
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":player"))

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.ui)

    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okio)
    implementation(libs.jna)
    implementation(libs.jna.platform)

    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.desktop {
    application {
        mainClass = "com.luc4n3x.levyra.desktop.app.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Levyra"
            packageVersion = levyraDesktopVersion
            description = "Levyra Desktop"
            copyright = "Copyright (c) LUC4N3X"
            vendor = "LUC4N3X"
            licenseFile.set(rootProject.file("../LICENSE"))
            includeAllModules = true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            windows {
                iconFile.set(rootProject.file("packaging/windows/levyra.ico"))
                menu = true
                menuGroup = "Levyra"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "0f3f2d8c-2f1e-4a7b-9a51-6d9c2a1f5b74"
            }
        }
    }
}
