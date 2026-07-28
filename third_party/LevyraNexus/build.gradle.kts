plugins {
    kotlin("jvm") version "2.4.10"
}

group = "com.github.LUC4N3X"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
