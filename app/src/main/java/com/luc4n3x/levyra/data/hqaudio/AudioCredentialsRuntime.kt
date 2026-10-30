package com.luc4n3x.levyra.data.hqaudio

/**
 * Account credentials must not be bundled, downloaded from public configuration,
 * or restored from credentials cached by older Levyra versions.
 */
internal object AudioCredentialsRuntime : AudioCredentialsSource {
    private val disabled = AudioCredentials.embedded()

    override suspend fun current(): AudioCredentials = disabled

    override suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials = disabled
}
