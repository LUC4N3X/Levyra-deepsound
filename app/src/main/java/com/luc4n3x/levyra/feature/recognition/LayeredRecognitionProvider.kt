package com.luc4n3x.levyra.feature.recognition

object RecognitionFallbackPolicy {
    fun allowsFallback(outcome: RecognitionOutcome): Boolean = when (outcome) {
        is RecognitionOutcome.Match -> false
        RecognitionOutcome.NoMatch -> true
        is RecognitionOutcome.Error -> when (outcome.kind) {
            RecognitionErrorKind.Network,
            RecognitionErrorKind.Timeout,
            RecognitionErrorKind.Unavailable -> true
            RecognitionErrorKind.Cancelled,
            RecognitionErrorKind.PermissionDenied,
            RecognitionErrorKind.Fingerprint -> false
        }
    }

    fun preferredOutcome(
        primary: RecognitionOutcome,
        fallback: RecognitionOutcome
    ): RecognitionOutcome = when {
        fallback is RecognitionOutcome.Match -> fallback
        primary is RecognitionOutcome.NoMatch -> primary
        fallback is RecognitionOutcome.NoMatch && primary is RecognitionOutcome.Error -> primary
        else -> fallback
    }
}

class LayeredRecognitionProvider(
    private val primary: RecognitionProvider,
    private val fallback: RecognitionProvider?
) : RecognitionProvider {
    override val id: String = if (fallback == null) primary.id else "${primary.id}+${fallback.id}"

    override suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome {
        val primaryOutcome = primary.identify(fingerprint)
        val secondary = fallback ?: return primaryOutcome
        if (!RecognitionFallbackPolicy.allowsFallback(primaryOutcome)) return primaryOutcome
        val fallbackOutcome = secondary.identify(fingerprint)
        return RecognitionFallbackPolicy.preferredOutcome(primaryOutcome, fallbackOutcome)
    }
}
