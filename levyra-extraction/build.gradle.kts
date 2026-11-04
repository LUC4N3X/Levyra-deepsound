plugins {
    alias(libs.plugins.kotlin.jvm)
}

val isFdroidBuild = providers.gradleProperty("levyraFdroidBuild")
    .map(String::toBoolean)
    .getOrElse(false)

kotlin {
    jvmToolchain(if (isFdroidBuild) 21 else 17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
