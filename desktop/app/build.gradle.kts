import java.util.Properties
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.Sync
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

val versionFile = rootProject.file("../gradle.properties")
val levyraDesktopVersion = Properties().apply {
    versionFile.inputStream().use(::load)
}.getProperty("levyraVersionName")?.trim()
    ?: error("levyraVersionName non trovato in gradle.properties")

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
            include("com/luc4n3x/levyra/ui/update/LevyraReleaseContent.kt")
            include("com/luc4n3x/levyra/ui/theme/LevyraTypeRhythm.kt")
            include("com/luc4n3x/levyra/update/AppUpdateProgress.kt")
            include("com/luc4n3x/levyra/update/ReleaseHighlights.kt")
            include("com/luc4n3x/levyra/domain/LevyraAudio.kt")
            include("com/luc4n3x/levyra/domain/SouthAsianMusic.kt")
            include("com/luc4n3x/levyra/ui/HomeSoundtrackPortraits.kt")
            include("com/luc4n3x/levyra/domain/ParametricEqualizer.kt")
            include("com/luc4n3x/levyra/domain/OrbitEqualizer.kt")
            include("com/luc4n3x/levyra/domain/PlaylistImportFailureKind.kt")
            include("com/luc4n3x/levyra/domain/DownloadQualityChoice.kt")
            include("com/luc4n3x/levyra/domain/NightListening.kt")
            include("com/luc4n3x/levyra/domain/SmartAudioProfiles.kt")
            include("com/luc4n3x/levyra/domain/SmartResume.kt")
            include("com/luc4n3x/levyra/domain/AdaptiveListening.kt")
            include("com/luc4n3x/levyra/domain/LyricsProviderPriority.kt")
            include("com/luc4n3x/levyra/feature/radio/RadioCategory.kt")
            include("com/luc4n3x/levyra/feature/radio/RadioFavoriteSort.kt")
            exclude("com/luc4n3x/levyra/ui/i18n/LevyraPersonalizedSearchStrings.kt")
            exclude("com/luc4n3x/levyra/ui/i18n/LevyraPersonalCollectionStrings.kt")
            exclude("com/luc4n3x/levyra/ui/i18n/LevyraSmartOfflineCount.kt")
            exclude("com/luc4n3x/levyra/ui/i18n/LevyraRadioLibraryStrings.kt")
            exclude("com/luc4n3x/levyra/ui/i18n/LevyraHomeCompositionStrings.kt")
            exclude("com/luc4n3x/levyra/ui/i18n/LevyraMoodStyleStrings.kt")
            exclude("com/luc4n3x/levyra/ui/i18n/LevyraFreshDiscoveryStrings.kt")
        }
        kotlin.source(sharedAndroidSources)
    }
}

val generatedComposeResources = layout.buildDirectory.dir("generated/composeResources/main")
val prepareComposeResources = tasks.register<Sync>("prepareComposeResources") {
    from(layout.projectDirectory.file("src/main/resources/icons/levyra.png"))
    from(layout.projectDirectory.file("../../app/src/main/res/drawable/levyra_logo.png"))
    from(layout.projectDirectory.file("../../app/src/main/res/drawable-nodpi/onboarding_levyra_brand.webp"))
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
    implementation(libs.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okio)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.dbus.core)
    runtimeOnly(libs.dbus.unixsocket)

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
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Levyra"
            packageVersion = levyraDesktopVersion.substringBefore('-').substringBefore('+')
            description = "Levyra Desktop"
            copyright = "Copyright (c) LUC4N3X"
            vendor = "LUC4N3X"
            licenseFile.set(rootProject.file("packaging/windows/TERMS.txt"))
            includeAllModules = true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            linux {
                iconFile.set(layout.projectDirectory.file("src/main/resources/icons/levyra.png"))
                packageName = "levyra"
                shortcut = true
                menuGroup = "AudioVideo"
                appCategory = "AudioVideo"
                debMaintainer = "LUC4N3X"
                rpmLicenseType = "GPL-3.0-only"
            }

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
