from pathlib import Path


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


resolver = Path("app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt")
hedge = Path("app/src/main/java/com/luc4n3x/levyra/data/LevyraStreamHedge.kt")
runtime = Path("app/src/main/java/com/luc4n3x/levyra/data/NewPipeRuntime.kt")

replace_once(
    resolver,
    'ClientProfile("ANDROID_VR", "1.65.10", "Android VR", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip", true, 0L, 1, false),',
    'ClientProfile("ANDROID_VR", "1.65.10", "Android VR", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip", true, 0L, 1, true),',
)

replace_once(
    resolver,
    '''        val sorted = candidates.sortedWith(\n            compareByDescending<ClientProfile> { profile -> clientHealth[profile.clientName]?.score ?: 50.0 }\n                .thenBy { profile -> clientHealth[profile.clientName]?.averageLatencyMs ?: Long.MAX_VALUE }\n                .thenBy { it.tier }\n        )''',
    '''        val sorted = candidates.sortedWith(\n            compareByDescending<ClientProfile> { it.requiresPoToken }\n                .thenByDescending { profile -> clientHealth[profile.clientName]?.score ?: 50.0 }\n                .thenBy { profile -> clientHealth[profile.clientName]?.averageLatencyMs ?: Long.MAX_VALUE }\n                .thenBy { it.tier }\n        )''',
)

replace_once(
    resolver,
    '        if (isTrustedGoogleVideoUrl(url)) return true',
    '        if (isTrustedGoogleVideoUrl(url) && url.containsQueryParameter("pot")) return true',
)

replace_once(
    resolver,
    '''        if (recovery.refreshSecurity) {\n            YoutubeLocalDecoder.notifyStreamRejected(track.source)\n        }''',
    '''        if (recovery.refreshSecurity) {\n            YoutubeLocalDecoder.notifyStreamRejected(track.source)\n            resolveScope.launch {\n                runCatchingPreservingCancellation {\n                    playbackSecurity.rotateIfNeeded(PlaybackBlockedException(reason))\n                }.onFailure { error ->\n                    Timber.w(error, "YouTube playback security refresh failed")\n                }\n            }\n        }''',
)

replace_once(
    resolver,
    '        val endpoint = "https://www.youtube.com/youtubei/v1/player?key=$apiKey&prettyPrint=false"',
    '''        val endpointHost = if (profile.clientName == "ANDROID_VR") {\n            "https://youtubei.googleapis.com"\n        } else {\n            "https://www.youtube.com"\n        }\n        val endpoint = "$endpointHost/youtubei/v1/player?key=$apiKey&prettyPrint=false"''',
)

replace_once(
    hedge,
    '    private const val VIDEO_INNER_TUBE_FALLBACK_MS = 2_500L',
    '    private const val VIDEO_INNER_TUBE_FALLBACK_MS = 0L',
)
replace_once(
    hedge,
    '''    fun extractorHedgeDelayMs(isVideoMode: Boolean, preferMp4Audio: Boolean): Long {\n        return 0L\n    }''',
    '''    fun extractorHedgeDelayMs(isVideoMode: Boolean, preferMp4Audio: Boolean): Long {\n        if (preferMp4Audio) return 0L\n        return if (isVideoMode) 900L else 600L\n    }''',
)

replace_once(
    runtime,
    'import org.schabi.newpipe.extractor.localization.Localization\n',
    'import org.schabi.newpipe.extractor.localization.Localization\nimport org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper\n',
)
replace_once(
    runtime,
    '''    private fun toOkHttpRequest(request: Request): okhttp3.Request {\n        val method = request.httpMethod().uppercase()\n        val data = request.dataToSend()\n        val body = when {''',
    '''    private fun toOkHttpRequest(request: Request): okhttp3.Request {\n        val method = request.httpMethod().uppercase()\n        val rawData = request.dataToSend()\n        val data = if (\n            rawData != null &&\n            method == "POST" &&\n            isYoutubePlayerEndpoint(request.url())\n        ) {\n            YoutubeParsingHelper.addSessionPoTokenToPlayerBody(\n                rawData,\n                NewPipe.getPreferredLocalization(),\n                NewPipe.getPreferredContentCountry()\n            )\n        } else {\n            rawData\n        }\n        val body = when {''',
)
replace_once(
    runtime,
    '''    private fun toOkHttpRequest(request: Request): okhttp3.Request {''',
    '''    private fun isYoutubePlayerEndpoint(url: String): Boolean {\n        return url.startsWith("https://www.youtube.com/youtubei/v1/player") ||\n            url.startsWith("https://youtubei.googleapis.com/youtubei/v1/player")\n    }\n\n    private fun toOkHttpRequest(request: Request): okhttp3.Request {''',
)

print("YouTube playback hotfix applied")
