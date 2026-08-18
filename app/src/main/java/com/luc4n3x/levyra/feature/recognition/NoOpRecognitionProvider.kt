package com.luc4n3x.levyra.feature.recognition

object NoOpRecognitionProvider : RecognitionProvider {
    override val id: String = "noop"

    override suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome =
        RecognitionOutcome.Error(RecognitionErrorKind.Unavailable)
}
