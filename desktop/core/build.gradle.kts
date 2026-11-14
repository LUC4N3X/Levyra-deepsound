plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val androidSourceRoot = rootProject.file("../app/src/main/java")
val sharedAudioSources = layout.buildDirectory.dir("generated/sharedAudio")
val prepareSharedAudioSources = tasks.register("prepareSharedAudioSources") {
    val sources = fileTree(androidSourceRoot) {
        include("com/luc4n3x/levyra/data/hqaudio/**/*.kt")
        exclude("**/HighQualityPlaybackCoordinator.kt")
        include("com/luc4n3x/levyra/data/PlaybackConcurrency.kt")
        include("com/luc4n3x/levyra/domain/HighQualityAudio.kt")
        include("com/luc4n3x/levyra/domain/PlaybackManifest.kt")
        include("com/luc4n3x/levyra/player/offline/TidalDashFlac.kt")
    }
    inputs.files(sources)
    outputs.dir(sharedAudioSources)
    doLast {
        sync { from(sources); into(sharedAudioSources) }
        sources.forEach { source ->
            var code = source.readText()
            if (source.name == "PlaybackManifest.kt") {
                val start = code.indexOf("enum class PlaybackDeliveryMethod")
                val end = code.indexOf("data class PlaybackStreamProvenance")
                check(start >= 0 && end > start)
                code = "package com.luc4n3x.levyra.domain\n\n" + code.substring(start, end)
            }
            if (source.name == "HighQualityMappingStore.kt") {
                code = code.replace("import android.content.Context", "")
                val start = code.indexOf("internal class SharedPreferencesMappingStorage")
                val end = code.indexOf("data class StoredAlternativeMapping")
                check(start >= 0 && end > start)
                code = code.removeRange(start, end)
            }
            code = code.replace("import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory",
                "import com.luc4n3x.levyra.desktop.core.stream.DesktopProviderNetwork as LevyraHttpClientFactory")
                .replace("import com.luc4n3x.levyra.data.network.LevyraNetworkConfiguration",
                    "import com.luc4n3x.levyra.desktop.core.stream.DesktopProviderNetwork as LevyraNetworkConfiguration")
                .replace("import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy",
                    "import com.luc4n3x.levyra.desktop.core.stream.DesktopProviderNetwork as SafeImageUrlPolicy")
                .replace("import timber.log.Timber",
                    "import com.luc4n3x.levyra.desktop.core.stream.DesktopProviderDiagnostics as Timber")
                .replace("import android.net.Uri", "")
                .replace("Uri.fromFile(file).toString()", "file.toURI().toASCIIString()")
            check(!code.contains("import android.")) { "Unadapted Android source: ${source.name}" }
            val output = sharedAudioSources.get().file(source.relativeTo(androidSourceRoot).path).asFile
            output.parentFile.mkdirs()
            output.writeText(code)
        }
    }
}

val sharedAudioFixtures = layout.buildDirectory.dir("generated/sharedAudioTest")
val prepareSharedAudioFixtures = tasks.register("prepareSharedAudioFixtures") {
    val fixture = rootProject.file("../app/src/test/java/com/luc4n3x/levyra/data/hqaudio/HighQualityTestFixtures.kt")
    inputs.file(fixture)
    outputs.dir(sharedAudioFixtures)
    doLast {
        var code = fixture.readText()
        val start = code.indexOf("internal fun playbackTrack(")
        val end = code.indexOf("internal class InMemoryMappingStorage")
        check(start >= 0 && end > start)
        code = code.removeRange(start, end)
        listOf("PlaybackStreamDescriptor", "PlaybackStreamKind", "ResolvedPlaybackManifest", "Track").forEach {
            code = code.replace("import com.luc4n3x.levyra.domain.$it", "")
        }
        val target = sharedAudioFixtures.get().file("HighQualityTestFixtures.kt").asFile
        target.parentFile.mkdirs()
        target.writeText(code)
    }
}

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("../third_party/LevyraNexus/src/main/kotlin"))
        kotlin.srcDir(sharedAudioSources)
    }
    sourceSets.named("test") {
        resources.srcDir(rootProject.file("../app/src/test/resources"))
        kotlin.srcDir(rootProject.file("../third_party/LevyraNexus/src/test/kotlin"))
        val sharedAudioTests = objects.sourceDirectorySet("sharedAudioTests", "Shared audio regression tests").apply {
            srcDir(rootProject.file("../app/src/test/java"))
            include("com/luc4n3x/levyra/data/hqaudio/**/*.kt")
            exclude("**/HighQualityPlaybackCoordinatorTest.kt")
            exclude("**/HighQualityTestFixtures.kt")
        }
        kotlin.source(sharedAudioTests)
        kotlin.srcDir(sharedAudioFixtures)
    }
}

tasks.named("compileKotlin") { dependsOn(prepareSharedAudioSources) }
tasks.named("compileTestKotlin") { dependsOn(prepareSharedAudioFixtures) }
tasks.withType<Test>().configureEach { workingDir(rootProject.projectDir.parentFile) }

dependencies {
    api(libs.levyra.extractor)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
