plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.levyra.extractor)
    api("com.github.LUC4N3X:LevyraNexus:1.0.0")
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    implementation(libs.okhttp.brotli)
    runtimeOnly(libs.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
