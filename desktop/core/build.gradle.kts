plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.levyra.extractor)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    implementation(libs.okhttp.brotli)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
