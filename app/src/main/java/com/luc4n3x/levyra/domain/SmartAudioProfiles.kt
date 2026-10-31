package com.luc4n3x.levyra.domain

enum class SmartAudioDeviceCategory(val storageValue: String) {
    SPEAKER("speaker"),
    WIRED("wired"),
    BLUETOOTH_HEADPHONES("bluetooth_headphones"),
    BLUETOOTH_SPEAKER("bluetooth_speaker"),
    USB("usb"),
    CAR("car"),
    OTHER("other");

    companion object {
        fun fromStorage(value: String?): SmartAudioDeviceCategory =
            entries.firstOrNull { it.storageValue == value } ?: OTHER
    }
}

enum class SmartAudioEqMode(val storageValue: String) {
    KEEP_GLOBAL("keep_global"),
    OFF("off"),
    PRESET("preset"),
    PARAMETRIC("parametric");

    companion object {
        fun fromStorage(value: String?): SmartAudioEqMode =
            entries.firstOrNull { it.storageValue == value } ?: KEEP_GLOBAL
    }
}

enum class SmartAudioNightMode(val storageValue: String) {
    KEEP_GLOBAL("keep_global"),
    OFF("off"),
    ON("on");

    companion object {
        fun fromStorage(value: String?): SmartAudioNightMode =
            entries.firstOrNull { it.storageValue == value } ?: KEEP_GLOBAL
    }
}

data class SmartAudioProfile(
    val deviceKey: String,
    val deviceName: String,
    val category: SmartAudioDeviceCategory,
    val eqMode: SmartAudioEqMode = SmartAudioEqMode.KEEP_GLOBAL,
    val presetId: String = LevyraAudioPresets.FLAT,
    val presetLabel: String = "",
    val bandLevels: List<Int> = LevyraAudioPresets.flatLevels,
    val bassBoost: Int = 0,
    val preampDb: Float = 0f,
    val parametricProfile: ParametricEqProfile? = null,
    val overrideReplayGain: Boolean = false,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val replayGainPreampDb: Float = 0f,
    val nightMode: SmartAudioNightMode = SmartAudioNightMode.KEEP_GLOBAL,
    val nightPreset: NightListeningPreset = NightListeningPreset.BALANCED
) {
    fun normalized(): SmartAudioProfile? {
        val key = deviceKey.trim().take(MAX_KEY_CHARS)
        if (key.isEmpty()) return null
        val parametric = parametricProfile?.normalized()
        return copy(
            deviceKey = key,
            deviceName = deviceName.trim().take(MAX_NAME_CHARS),
            eqMode = if (eqMode == SmartAudioEqMode.PARAMETRIC && parametric == null) SmartAudioEqMode.KEEP_GLOBAL else eqMode,
            bandLevels = bandLevels.takeIf { it.size == LevyraAudioPresets.bandCount }
                ?.map { it.coerceIn(-100, 100) }
                ?: LevyraAudioPresets.flatLevels,
            bassBoost = bassBoost.coerceIn(0, 100),
            preampDb = preampDb.takeIf(Float::isFinite)?.coerceIn(MIN_PREAMP_DB, MAX_PREAMP_DB) ?: 0f,
            parametricProfile = parametric,
            replayGainPreampDb = replayGainPreampDb.takeIf(Float::isFinite)
                ?.coerceIn(MIN_REPLAY_GAIN_PREAMP_DB, MAX_REPLAY_GAIN_PREAMP_DB) ?: 0f
        )
    }

    companion object {
        const val MAX_KEY_CHARS = 96
        const val MAX_NAME_CHARS = 80
        const val MAX_PROFILES = 32
        const val MIN_PREAMP_DB = -12f
        const val MAX_PREAMP_DB = 3f
        const val MIN_REPLAY_GAIN_PREAMP_DB = -12f
        const val MAX_REPLAY_GAIN_PREAMP_DB = 6f
    }
}

object SmartAudioResolver {
    fun profileFor(settings: LevyraAudioSettings, deviceKey: String?): SmartAudioProfile? {
        if (!settings.smartAudioProfilesEnabled || deviceKey.isNullOrEmpty()) return null
        return settings.smartAudioProfiles.firstOrNull { it.deviceKey == deviceKey }
    }

    fun resolve(settings: LevyraAudioSettings, deviceKey: String?): LevyraAudioSettings {
        val profile = profileFor(settings, deviceKey) ?: return settings
        val withEq = when (profile.eqMode) {
            SmartAudioEqMode.KEEP_GLOBAL -> settings
            SmartAudioEqMode.OFF -> settings.copy(equalizerEnabled = false, parametricEqualizerEnabled = false)
            SmartAudioEqMode.PRESET -> settings.copy(
                equalizerEnabled = true,
                presetId = profile.presetId,
                bandLevels = profile.bandLevels,
                bassBoost = profile.bassBoost,
                preampDb = profile.preampDb,
                parametricEqualizerEnabled = false
            )
            SmartAudioEqMode.PARAMETRIC -> settings.copy(
                equalizerEnabled = false,
                parametricEqualizerEnabled = true,
                activeParametricProfile = profile.parametricProfile
            )
        }
        val withReplayGain = if (profile.overrideReplayGain) {
            withEq.copy(
                replayGainMode = profile.replayGainMode,
                replayGainEnabled = profile.replayGainMode != ReplayGainMode.OFF,
                replayGainPreampDb = profile.replayGainPreampDb
            )
        } else {
            withEq
        }
        return when (profile.nightMode) {
            SmartAudioNightMode.KEEP_GLOBAL -> withReplayGain
            SmartAudioNightMode.OFF -> withReplayGain.copy(nightListeningEnabled = false)
            SmartAudioNightMode.ON -> withReplayGain.copy(
                nightListeningEnabled = true,
                nightListeningPreset = profile.nightPreset
            )
        }.normalized()
    }

    fun upsert(profiles: List<SmartAudioProfile>, profile: SmartAudioProfile): List<SmartAudioProfile> {
        val clean = profile.normalized() ?: return profiles
        val others = profiles.filterNot { it.deviceKey == clean.deviceKey }
        return (others + clean).takeLast(SmartAudioProfile.MAX_PROFILES)
    }
}
