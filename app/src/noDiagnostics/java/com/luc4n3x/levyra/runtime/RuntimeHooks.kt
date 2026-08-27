package com.luc4n3x.levyra.runtime

import android.content.Context
import androidx.compose.runtime.Composable
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import androidx.media3.exoplayer.ExoPlayer

internal object RuntimeSignal {
    const val PLAYER_CREATED = 1
    const val PLAYER_RELEASED = 2
    const val PLAYER_PREPARE = 3
    const val PLAYER_TRANSITION = 4
    const val PLAYER_ERROR = 5
    const val PLAYER_RECOVERY = 6
    const val PLAYER_STATE = 7
    const val MODE_AUDIO = 0
    const val MODE_VIDEO = 1
    const val MODE_DECORATIVE = 2
    const val OUTCOME_SUCCESS = 1
    const val OUTCOME_FAILURE = 2
    const val OUTCOME_TIMEOUT = 3
    const val OUTCOME_CANCELLED = 4
    const val CACHE_HIT = 1
    const val CACHE_MISS = 2
    const val CACHE_EVICTION = 3
    const val CACHE_PREFETCH = 4
    const val DSP_CREATED = 1
    const val DSP_RECREATED = 2
    const val DSP_RELEASED = 3
    const val CANVAS_STARTED = 1
    const val CANVAS_FIRST_FRAME = 2
    const val CANVAS_FALLBACK = 3
    const val CANVAS_RESTARTED = 4
    const val CANVAS_STOPPED = 5
    const val NETWORK_CONNECT = 1
    const val NETWORK_RESOLVE = 2
    const val NETWORK_CONFIG = 3
    const val NETWORK_HTTP = 4
    const val HOT_PLAYER_CREATE = 1
    const val HOT_PLAYER_PREPARE = 2
    const val HOT_RESOLVER_ATTEMPT = 3
    const val HOT_FALLBACK = 4
    const val HOT_NETWORK_RETRY = 5
    const val HOT_CACHE_ACCESS = 6
    const val HOT_DSP_RECREATE = 7
    const val HOT_CANVAS_RESTART = 8
    const val FAILURE_NETWORK = 14
    const val FAILURE_TIMEOUT = 15
    const val FAILURE_UNKNOWN = 17
}

internal object RuntimeHooks {
    fun start(context: Context) = Unit
    fun attachPlayer(player: ExoPlayer) = Unit
    fun player(action: Int, value: Int = 0, mode: Int = RuntimeSignal.MODE_AUDIO, failure: Int = -1) = Unit
    fun resolver(
        mode: Int,
        strategy: Int,
        client: Int,
        attempt: Int,
        latencyMs: Long,
        outcome: Int,
        failure: Int,
        manifest: ResolvedPlaybackManifest? = null
    ) = Unit
    fun cache(action: Int) = Unit
    fun dsp(action: Int) = Unit
    fun canvas(action: Int) = Unit
    fun network(host: String, category: Int, latencyMs: Long, outcome: Int, statusCode: Int = 0, retry: Int = 0, redirects: Int = 0, failure: Int = -1) = Unit
    fun hot(operation: Int) = Unit

    @Composable
    fun internalPanelOverlay() = Unit
}
