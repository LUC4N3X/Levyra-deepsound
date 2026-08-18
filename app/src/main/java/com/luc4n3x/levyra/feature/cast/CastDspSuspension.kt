package com.luc4n3x.levyra.feature.cast

data class LocalDspSettings(
    val equalizerEnabled: Boolean,
    val bassBoost: Int,
    val virtualizer: Int,
    val preampDb: Float,
    val limiterEnabled: Boolean,
    val normalizationEnabled: Boolean,
    val crossfadeSeconds: Int,
    val autoMixEnabled: Boolean
)

data class SuspendedDspSnapshot(
    val original: LocalDspSettings,
    val suspended: LocalDspSettings
)

object CastDspSuspension {
    fun suspendForRemote(current: LocalDspSettings): SuspendedDspSnapshot {
        val suspended = LocalDspSettings(
            equalizerEnabled = false,
            bassBoost = 0,
            virtualizer = 0,
            preampDb = 0f,
            limiterEnabled = false,
            normalizationEnabled = false,
            crossfadeSeconds = 0,
            autoMixEnabled = false
        )
        return SuspendedDspSnapshot(original = current, suspended = suspended)
    }

    fun restoreLocal(snapshot: SuspendedDspSnapshot): LocalDspSettings = snapshot.original
}
