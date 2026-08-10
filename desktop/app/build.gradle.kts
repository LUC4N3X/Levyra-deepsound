import java.util.Properties
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.Sync
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

val desktopVersionFile = rootProject.file("version.properties")
val desktopVersionProperties = Properties().apply {
    require(desktopVersionFile.isFile) {
        "desktop/version.properties non trovato"
    }
    desktopVersionFile.inputStream().use(::load)
}

val levyraDesktopVersion = providers.gradleProperty("levyraDesktopVersion").getOrElse(
    desktopVersionProperties.getProperty("levyraDesktopVersion")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: error("levyraDesktopVersion non trovato in desktop/version.properties")
)

require(levyraDesktopVersion.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+([-.+][0-9A-Za-z.-]+)?$"))) {
    "Versione Levyra Desktop non valida: $levyraDesktopVersion"
}

val generatedVersionResources = layout.buildDirectory.dir("generated/resources/desktopVersion")
val generateDesktopVersionResource = tasks.register("generateDesktopVersionResource") {
    inputs.property("levyraDesktopVersion", levyraDesktopVersion)
    outputs.dir(generatedVersionResources)
    doLast {
        val output = generatedVersionResources.get().file("levyra-desktop-version.properties").asFile
        output.parentFile.mkdirs()
        output.writeText("levyraDesktopVersion=$levyraDesktopVersion\n")
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateDesktopVersionResource)
    from(generatedVersionResources)
}

kotlin {
    sourceSets.named("main") {
        val sharedAndroidSources = objects.sourceDirectorySet(
            "sharedAndroidSources",
            "Android sources shared with Levyra Desktop"
        ).apply {
            srcDir(rootProject.file("../app/src/main/java"))
            include("com/luc4n3x/levyra/ui/i18n/**/*.kt")
            include("com/luc4n3x/levyra/domain/LevyraAudio.kt")
            include("com/luc4n3x/levyra/domain/PlaylistImportFailureKind.kt")
        }
        kotlin.source(sharedAndroidSources)
    }
}

val generatedComposeResources = layout.buildDirectory.dir("generated/composeResources/main")
val prepareComposeResources = tasks.register<Sync>("prepareComposeResources") {
    from(layout.projectDirectory.file("src/main/resources/icons/levyra.png"))
    into(generatedComposeResources.map { it.dir("drawable") })
}

dependencies {
    implementation(project(":core"))
    implementation(project(":player"))

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.resources)

    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okio)
    implementation(libs.jna)
    implementation(libs.jna.platform)

    implementation(libs.slf4j.api)

    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.luc4n3x.levyra.desktop.app.generated.resources"
    customDirectory("main", prepareComposeResources.map { generatedComposeResources.get() })
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
                dirChooser = false
                perUserInstall = true
                upgradeUuid = "0f3f2d8c-2f1e-4a7b-9a51-6d9c2a1f5b74"
            }
        }
    }
}
