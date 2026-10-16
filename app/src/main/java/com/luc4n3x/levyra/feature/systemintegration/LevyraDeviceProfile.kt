package com.luc4n3x.levyra.feature.systemintegration

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.lang.reflect.Method
import java.util.Locale

internal const val OPLUS_ROM_VERSION_PROPERTY = "ro.build.version.oplusrom"
private const val HYPER_OS_VERSION_CODE_PROPERTY = "ro.mi.os.version.code"
private const val HYPER_OS_VERSION_NAME_PROPERTY = "ro.mi.os.version.name"
private const val MIUI_VERSION_NAME_PROPERTY = "ro.miui.ui.version.name"
private const val VIVO_OS_NAME_PROPERTY = "ro.vivo.os.name"
private const val VIVO_OS_VERSION_PROPERTY = "ro.vivo.os.version"
private const val VIVO_OS_DISPLAY_PROPERTY = "ro.vivo.os.build.display.id"
private const val ONE_UI_VERSION_PROPERTY = "ro.build.version.oneui"
private const val MAGIC_OS_VERSION_PROPERTY = "ro.build.version.magic"
private const val HYPER_OS_FOCUS_PROTOCOL_SETTING = "notification_focus_protocol"
private const val HYPER_OS_SUPER_ISLAND_PROTOCOL = 3
private const val ATOMIC_ISLAND_MIN_SDK = 34
private const val NOW_BAR_MIN_ONE_UI = 70_000

internal enum class LevyraDeviceFamily {
    XIAOMI,
    VIVO,
    OPLUS,
    SAMSUNG,
    HONOR,
    GOOGLE,
    GENERIC
}

internal enum class LevyraDeviceFeature {
    SUPER_ISLAND,
    ATOMIC_ISLAND,
    OPLUS_TIMED_LYRICS,
    NOW_BAR
}

internal data class LevyraDeviceSignals(
    val manufacturer: String,
    val brand: String,
    val sdkInt: Int,
    val androidRelease: String,
    val hyperOsVersionCode: String = "",
    val hyperOsVersionName: String = "",
    val miuiVersionName: String = "",
    val focusProtocol: Int = 0,
    val vivoOsName: String = "",
    val vivoOsVersion: String = "",
    val vivoOsDisplay: String = "",
    val oneUiVersion: String = "",
    val oplusRomVersion: String = "",
    val magicOsVersion: String = ""
)

internal data class LevyraDeviceProfile(
    val family: LevyraDeviceFamily,
    val manufacturer: String,
    val systemName: String,
    val features: Set<LevyraDeviceFeature>
) {
    fun supports(feature: LevyraDeviceFeature): Boolean = feature in features
}

internal fun resolveLevyraDeviceProfile(signals: LevyraDeviceSignals): LevyraDeviceProfile {
    val identity = "${signals.manufacturer} ${signals.brand}".lowercase(Locale.ROOT)
    val hyperOsVersion = hyperOsMajorVersion(signals)
    val oneUi = signals.oneUiVersion.trim().toIntOrNull()?.takeIf { it > 0 }
    val vivoOs = "${signals.vivoOsName} ${signals.vivoOsDisplay}".lowercase(Locale.ROOT)
    val family = when {
        isOplusIdentity(identity) || signals.oplusRomVersion.isNotBlank() -> LevyraDeviceFamily.OPLUS
        hyperOsVersion != null || listOf("xiaomi", "redmi", "poco").any(identity::contains) -> LevyraDeviceFamily.XIAOMI
        listOf("vivo", "iqoo").any(identity::contains) -> LevyraDeviceFamily.VIVO
        identity.contains("samsung") || oneUi != null -> LevyraDeviceFamily.SAMSUNG
        listOf("honor", "huawei").any(identity::contains) || signals.magicOsVersion.isNotBlank() -> LevyraDeviceFamily.HONOR
        identity.contains("google") -> LevyraDeviceFamily.GOOGLE
        else -> LevyraDeviceFamily.GENERIC
    }
    val features = buildSet {
        if (
            family == LevyraDeviceFamily.XIAOMI &&
            signals.sdkInt >= Build.VERSION_CODES.O_MR1 &&
            signals.focusProtocol >= HYPER_OS_SUPER_ISLAND_PROTOCOL
        ) {
            add(LevyraDeviceFeature.SUPER_ISLAND)
        }
        if (
            family == LevyraDeviceFamily.VIVO &&
            signals.sdkInt >= ATOMIC_ISLAND_MIN_SDK &&
            isOriginOs(vivoOs)
        ) {
            add(LevyraDeviceFeature.ATOMIC_ISLAND)
        }
        if (family == LevyraDeviceFamily.OPLUS) add(LevyraDeviceFeature.OPLUS_TIMED_LYRICS)
        if (family == LevyraDeviceFamily.SAMSUNG && oneUi != null && oneUi >= NOW_BAR_MIN_ONE_UI) {
            add(LevyraDeviceFeature.NOW_BAR)
        }
    }
    return LevyraDeviceProfile(
        family = family,
        manufacturer = signals.manufacturer.trim().ifBlank { signals.brand.trim() },
        systemName = systemName(family, identity, signals, hyperOsVersion, oneUi, vivoOs),
        features = features
    )
}

internal fun isOplusIdentity(identity: String): Boolean =
    listOf("oppo", "oneplus", "realme").any(identity.lowercase(Locale.ROOT)::contains)

internal object LevyraDeviceProfiles {
    @Volatile
    private var cached: LevyraDeviceProfile? = null

    fun current(context: Context): LevyraDeviceProfile =
        cached ?: resolveLevyraDeviceProfile(readLevyraDeviceSignals(context.applicationContext)).also { cached = it }
}

internal fun readLevyraDeviceSignals(context: Context): LevyraDeviceSignals = LevyraDeviceSignals(
    manufacturer = Build.MANUFACTURER.orEmpty(),
    brand = Build.BRAND.orEmpty(),
    sdkInt = Build.VERSION.SDK_INT,
    androidRelease = Build.VERSION.RELEASE.orEmpty(),
    hyperOsVersionCode = levyraSystemProperty(HYPER_OS_VERSION_CODE_PROPERTY),
    hyperOsVersionName = levyraSystemProperty(HYPER_OS_VERSION_NAME_PROPERTY),
    miuiVersionName = levyraSystemProperty(MIUI_VERSION_NAME_PROPERTY),
    focusProtocol = runCatching {
        Settings.System.getInt(context.contentResolver, HYPER_OS_FOCUS_PROTOCOL_SETTING, 0)
    }.getOrDefault(0),
    vivoOsName = levyraSystemProperty(VIVO_OS_NAME_PROPERTY),
    vivoOsVersion = levyraSystemProperty(VIVO_OS_VERSION_PROPERTY),
    vivoOsDisplay = levyraSystemProperty(VIVO_OS_DISPLAY_PROPERTY),
    oneUiVersion = levyraSystemProperty(ONE_UI_VERSION_PROPERTY),
    oplusRomVersion = levyraSystemProperty(OPLUS_ROM_VERSION_PROPERTY),
    magicOsVersion = levyraSystemProperty(MAGIC_OS_VERSION_PROPERTY)
)

private val systemPropertyGetter: Method? by lazy {
    runCatching {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
    }.getOrNull()
}

internal fun levyraSystemProperty(name: String): String =
    runCatching { systemPropertyGetter?.invoke(null, name) as? String }.getOrNull().orEmpty().trim()

private fun hyperOsMajorVersion(signals: LevyraDeviceSignals): Int? =
    signals.hyperOsVersionCode.toIntOrNull()?.takeIf { it > 0 }
        ?: HYPER_OS_NAME_PATTERN.find(signals.hyperOsVersionName)?.groupValues?.get(1)?.toIntOrNull()

private fun isOriginOs(vivoOs: String): Boolean =
    !vivoOs.contains("funtouch") && (vivoOs.contains("origin") || vivoOs.contains("vos"))

private fun systemName(
    family: LevyraDeviceFamily,
    identity: String,
    signals: LevyraDeviceSignals,
    hyperOsVersion: Int?,
    oneUi: Int?,
    vivoOs: String
): String = when (family) {
    LevyraDeviceFamily.XIAOMI -> when {
        hyperOsVersion != null -> "HyperOS $hyperOsVersion"
        signals.miuiVersionName.isNotBlank() -> "MIUI ${signals.miuiVersionName.removePrefix("V")}"
        else -> androidName(signals)
    }
    LevyraDeviceFamily.VIVO -> when {
        isOriginOs(vivoOs) -> listOf("OriginOS", leadingMajor(signals.vivoOsVersion)).filter(String::isNotBlank).joinToString(" ")
        vivoOs.contains("funtouch") -> "Funtouch OS"
        else -> androidName(signals)
    }
    LevyraDeviceFamily.OPLUS -> when {
        identity.contains("realme") -> "realme UI"
        identity.contains("oneplus") -> listOf("OxygenOS", leadingMajor(signals.oplusRomVersion)).filter(String::isNotBlank).joinToString(" ")
        else -> listOf("ColorOS", leadingMajor(signals.oplusRomVersion)).filter(String::isNotBlank).joinToString(" ")
    }
    LevyraDeviceFamily.SAMSUNG -> oneUi?.let { "One UI ${it / 10_000}.${it / 100 % 100}" } ?: androidName(signals)
    LevyraDeviceFamily.HONOR -> MAGIC_OS_PATTERN.find(signals.magicOsVersion)?.groupValues?.get(1)
        ?.let { "MagicOS $it" }
        ?: androidName(signals)
    LevyraDeviceFamily.GOOGLE,
    LevyraDeviceFamily.GENERIC -> androidName(signals)
}

private fun androidName(signals: LevyraDeviceSignals): String =
    listOf("Android", signals.androidRelease.trim()).filter(String::isNotBlank).joinToString(" ")

private fun leadingMajor(version: String): String =
    LEADING_MAJOR_PATTERN.find(version)?.groupValues?.get(1).orEmpty()

private val HYPER_OS_NAME_PATTERN = Regex("""OS(\d+)""")
private val MAGIC_OS_PATTERN = Regex("""(\d+(?:\.\d+)?)""")
private val LEADING_MAJOR_PATTERN = Regex("""^[Vv]?(\d+)""")
