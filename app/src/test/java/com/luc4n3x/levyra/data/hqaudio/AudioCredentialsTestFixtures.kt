package com.luc4n3x.levyra.data.hqaudio

/**
 * Synthetic credentials for isolated tests only. No production credentials.
 */
internal fun credentialDocument(revision: Long = 1, token: String = "new-qobuz"): String = """
    {"schemaVersion":1,"revision":$revision,
     "qobuz":{"accounts":[{"userAuthToken":"$token","appId":"123","appSecret":"test-secret","userId":"42"}]},
     "tidal":{"refreshTokens":["new-tidal"],"tvClientId":"tv-id","tvClientSecret":"tv-secret","mobileClientId":"mobile-id"}}
""".trimIndent()

internal class FixedAudioCredentialsSource(
    private val snapshot: AudioCredentials = requireNotNull(AudioCredentials.parse(credentialDocument()))
) : AudioCredentialsSource {
    override suspend fun current(): AudioCredentials = snapshot
    override suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials = snapshot
}
