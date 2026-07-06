import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licensee)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun envOrProperty(name: String, propertyName: String = name): String {
    val gradleProperty = findProperty(propertyName) as? String
    val localProperty = if (localProperties.containsKey(propertyName)) {
        localProperties.getProperty(propertyName)
    } else {
        null
    }
    val environmentValue = System.getenv(name)
    return (gradleProperty ?: localProperty ?: environmentValue ?: "").trim()
}

fun buildConfigString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    return "\"$escaped\""
}

fun isReleaseTaskRequested(): Boolean {
    return gradle.startParameter.taskNames.any { task ->
        task.contains("Release", ignoreCase = true) ||
            task == "bundle" ||
            task == "assemble"
    }
}

val youtubeInnertubeApiKey = envOrProperty("YOUTUBE_INNERTUBE_API_KEY", "youtubeInnertubeApiKey")
val releaseStoreFilePath = envOrProperty("LEVYRA_KEYSTORE_FILE", "levyraStoreFile").ifBlank { "app/levyra-release.jks" }
val releaseStorePassword = envOrProperty("LEVYRA_KEYSTORE_PASSWORD", "levyraStorePassword")
val releaseKeyAlias = envOrProperty("LEVYRA_KEY_ALIAS", "levyraKeyAlias")
val releaseKeyPassword = envOrProperty("LEVYRA_KEY_PASSWORD", "levyraKeyPassword")
val releaseStoreFile = rootProject.file(releaseStoreFilePath)

if (isReleaseTaskRequested() && youtubeInnertubeApiKey.isBlank()) {
    throw GradleException("Missing YOUTUBE_INNERTUBE_API_KEY. Set it as a GitHub Actions secret or in local.properties as youtubeInnertubeApiKey.")
}

if (isReleaseTaskRequested() && (!releaseStoreFile.isFile || releaseStorePassword.isBlank() || releaseKeyAlias.isBlank() || releaseKeyPassword.isBlank())) {
    throw GradleException("Missing release signing config. Set LEVYRA_KEYSTORE_BASE64, LEVYRA_KEYSTORE_PASSWORD, LEVYRA_KEY_ALIAS and LEVYRA_KEY_PASSWORD in GitHub Actions secrets.")
}

fun normalizedVersionName(value: String): String {
    val clean = value.trim().removePrefix("v").removePrefix("V")
    val match = Regex("\\d+(?:\\.\\d+){0,3}(?:[-+][0-9A-Za-z.-]+)?").find(clean)?.value
    return match ?: clean.ifBlank { "2.3.1" }
}

fun generatedVersionCode(versionName: String): Int {
    val parts = Regex("\\d+")
        .findAll(versionName)
        .mapNotNull { it.value.toIntOrNull() }
        .take(4)
        .toList()
    val major = parts.getOrElse(0) { 0 }.coerceIn(0, 999)
    val minor = parts.getOrElse(1) { 0 }.coerceIn(0, 99)
    val patch = parts.getOrElse(2) { 0 }.coerceIn(0, 99)
    val build = parts.getOrElse(3) { 0 }.coerceIn(0, 99)
    return major * 1_000_000 + minor * 10_000 + patch * 100 + build
}

fun githubTagVersionName(): String? {
    val refType = System.getenv("GITHUB_REF_TYPE").orEmpty()
    val refName = System.getenv("GITHUB_REF_NAME").orEmpty()
    val ref = System.getenv("GITHUB_REF").orEmpty()
    return when {
        refType == "tag" && refName.isNotBlank() -> refName
        ref.startsWith("refs/tags/") -> ref.substringAfterLast("/")
        else -> null
    }
}

val levyraVersionName = normalizedVersionName(
    (findProperty("levyraVersionName") as? String)
        ?: System.getenv("LEVYRA_VERSION_NAME")
        ?: githubTagVersionName()
        ?: "2.3.1"
)

val levyraVersionCode = ((findProperty("levyraVersionCode") as? String)
    ?: System.getenv("LEVYRA_VERSION_CODE"))
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: generatedVersionCode(levyraVersionName)

android {
    namespace = "com.luc4n3x.levyra"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.luc4n3x.levyra"
        minSdk = 26
        targetSdk = 35
        versionCode = levyraVersionCode
        versionName = levyraVersionName
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "UPDATE_REPOSITORY", "\"LUC4N3X/Levyra-deepsound\"")
        buildConfigField("String", "UPDATE_LATEST_URL", "\"https://api.github.com/repos/LUC4N3X/Levyra-deepsound/releases/latest\"")
        buildConfigField("String", "YOUTUBE_INNERTUBE_API_KEY", buildConfigString(youtubeInnertubeApiKey))
    }

    signingConfigs {
        getByName("debug")
        create("release") {
            storeFile = releaseStoreFile
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // The whole player layer is built on media3, whose APIs are annotated
        // @UnstableApi. UnsafeOptInUsageError targets library authors who expose
        // unstable APIs to consumers and is redundant for an app that
        // deliberately depends on media3, so disable just this check while
        // keeping every other lint rule enforced.
        disable += "UnsafeOptInUsageError"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/**"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.okhttp.brotli)
    implementation(libs.newpipe.extractor)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(libs.shimmer)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
    testImplementation(libs.junit)
    testImplementation(libs.json)
}
