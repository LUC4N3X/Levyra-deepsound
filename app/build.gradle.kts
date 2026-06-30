plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.licensee)
}

android {
    namespace = "com.luc4n3x.levyra"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.luc4n3x.levyra"
        minSdk = 26
        targetSdk = 35
        versionCode = 24
        versionName = "1.2.0"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "UPDATE_REPOSITORY", "\"LUC4N3X/Levyra-deepsound\"")
        buildConfigField("String", "UPDATE_LATEST_URL", "\"https://api.github.com/repos/LUC4N3X/Levyra-deepsound/releases/latest\"")
    }

    signingConfigs {
        getByName("debug")
        create("release") {
            storeFile = rootProject.file("app/levyra-release.jks")
            storePassword = "levyra2026"
            keyAlias = "levyra"
            keyPassword = "levyra2026"
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
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation("androidx.media3:media3-datasource:1.5.1")
    implementation(libs.androidx.media3.database)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.newpipe.extractor)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs.nio)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)
    debugImplementation(libs.leakcanary.android)
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allow("ISC")
    allow("JSON")
    allowDependency("com.github.teamnewpipe", "NewPipeExtractor", libs.versions.newpipe.get())
    allowDependency("com.github.TeamNewPipe", "NewPipeExtractor", libs.versions.newpipe.get())
    allowDependency("com.github.TeamNewPipe.NewPipeExtractor", "extractor", libs.versions.newpipe.get())
    allowDependency("com.github.TeamNewPipe.NewPipeExtractor", "timeago-parser", libs.versions.newpipe.get())
}

