plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("../third_party/LevyraNexus/src/main/kotlin"))
    }
    sourceSets.named("test") {
        kotlin.srcDir(rootProject.file("../third_party/LevyraNexus/src/test/kotlin"))
    }
}

dependencies {
    api(libs.levyra.extractor)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    implementation(libs.okhttp.brotli)
    runtimeOnly(libs.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
