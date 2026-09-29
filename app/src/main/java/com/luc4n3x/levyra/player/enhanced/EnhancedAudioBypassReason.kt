package com.luc4n3x.levyra.player.enhanced

/**
 * Diagnostic reasons for bypassing Levyra Enhanced Audio.
 *
 * If any condition prevents safe, high-fidelity restoration, the processor
 * immediately bypasses to pristine passthrough of the decoded source.
 */
enum class EnhancedAudioBypassReason(val label: String) {
    USER_DISABLED("Disabled by user"),
    UNSUPPORTED_FORMAT("Unsupported audio format"),
    ENGINE_NOT_READY("Engine not ready"),
    MODEL_LOAD_FAILED("Model load failed"),
    CPU_OVERLOAD("CPU overload detected"),
    LATENCY_TOO_HIGH("Latency exceeded budget"),
    INSUFFICIENT_CONFIDENCE("Low deficit confidence"),
    ALREADY_LOSSLESS("Source is already lossless"),
    CAST_OR_REMOTE_PLAYBACK("Remote / Cast playback active"),
    UNSUPPORTED_PLAYBACK_PATH("Unsupported playback path"),
    INTERNAL_ERROR("Internal processing error")
}
