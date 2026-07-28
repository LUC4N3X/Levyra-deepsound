plugins {
    kotlin("jvm") version "2.4.10"
}

group = "com.github.LUC4N3X"
version = "1.0.0"

val isFdroidBuild = providers.gradleProperty("levyraFdroidBuild")
    .map(String::toBoolean)
    .getOrElse(false)

kotlin {
    jvmToolchain(if (isFdroidBuild) 21 else 17)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
