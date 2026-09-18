plugins {
    alias(libs.plugins.android.library)
}

val isFdroidBuild = providers.gradleProperty("levyraFdroidBuild")
    .map(String::toBoolean)
    .getOrElse(false)
val nativeAudioRequested = providers.gradleProperty("levyraNativeAudio")
    .map(String::toBoolean)
    .getOrElse(false)
val nativeAudioEnabled = nativeAudioRequested && !isFdroidBuild
val nativeAudioMinApi = 26
val nativeAudioAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val nativeDepsDir = rootProject.layout.projectDirectory.dir(".gradle/levyra-native-deps")

android {
    namespace = "com.luc4n3x.levyra.nativeaudio"
    compileSdk = 37

    defaultConfig {
        minSdk = nativeAudioMinApi
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("boolean", "NATIVE_AUDIO_BUNDLED", nativeAudioEnabled.toString())
        if (nativeAudioEnabled) {
            ndk {
                abiFilters += nativeAudioAbis
            }
            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DANDROID_STL=c++_static",
                        "-DLEVYRA_NATIVE_DEPS_DIR=${nativeDepsDir.asFile.absolutePath}"
                    )
                }
            }
        }
    }

    if (nativeAudioEnabled) {
        ndkVersion = "28.2.13676358"
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        disable += "UnsafeOptInUsageError"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(if (isFdroidBuild) 21 else 17)
}

if (nativeAudioEnabled) {
    val prepareNativeAudioDependencies = tasks.register<Exec>("prepareNativeAudioDependencies") {
        val script = file("native-deps/prepare_native_deps.sh")
        val ndkDirectory = androidComponents.sdkComponents.ndkDirectory
        inputs.file(script)
        inputs.property("abis", nativeAudioAbis)
        inputs.property("minApi", nativeAudioMinApi)
        outputs.dir(nativeDepsDir)
        executable = "bash"
        argumentProviders.add(
            CommandLineArgumentProvider {
                listOf(
                    script.absolutePath,
                    nativeDepsDir.asFile.absolutePath,
                    ndkDirectory.get().asFile.absolutePath,
                    nativeAudioMinApi.toString()
                ) + nativeAudioAbis
            }
        )
    }
    tasks.configureEach {
        if (name.contains("CMake") || name.startsWith("externalNativeBuild")) {
            dependsOn(prepareNativeAudioDependencies)
        }
    }
}

dependencies {
    api(libs.androidx.media3.exoplayer)
    api(libs.androidx.media3.decoder)
    implementation(libs.androidx.annotation)
    testImplementation(libs.junit)
}
