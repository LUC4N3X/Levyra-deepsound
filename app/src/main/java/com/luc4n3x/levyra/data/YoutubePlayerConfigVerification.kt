package com.luc4n3x.levyra.data

internal enum class YoutubeConfigVerdict {
    ACCEPTED,
    REJECTED,
    INCONCLUSIVE
}

internal data class YoutubeConfigVerification(
    val verdict: YoutubeConfigVerdict,
    val reason: String
) {
    val accepted: Boolean
        get() = verdict == YoutubeConfigVerdict.ACCEPTED

    val provesConfigWrong: Boolean
        get() = verdict == YoutubeConfigVerdict.REJECTED
}

internal data class YoutubeCipherProbe(
    val input: String,
    val output: String
)

internal object YoutubePlayerConfigVerifier {

    val SIGNATURE_PROBES: List<String> = listOf(
        "AOq0QJ8wRgIhAK7VpLm2QcWnT4xYzB9dRfGhJkLmNpQrStUvWxYz0123456789AiEA" +
            "4bC6dEfGhIjKlMnOpQrStUvWxYz9876543210ZyXwVuTsRqPoNmLkJiHgFeDcBaQw" +
            "ErTyUiOpAsDfGhJkLzXcVbNm0192837465",
        "AOq0QJ8wRQIgZ9yXwVuTsRqPoNmLkJiHgFeDcBa87654321ZyXwVuTsRqPoNmLkJiH" +
            "gFeDcBaQwErTyUiOpAsDfGhJkLzXcVbNm5647382910AbCdEfGhIjKlMnOpQrStUv" +
            "WxYz0123456789QwErTyUiOpAsDfGhJkL"
    )

    val THROTTLING_PROBES: List<String> = listOf(
        "1PuFuHiVpM4tGJyMxq",
        "9bTrXk2QwEr7YuIoPa"
    )

    private val signatureCharset = Regex("^[A-Za-z0-9_=.-]+$")
    private val throttlingCharset = Regex("^[A-Za-z0-9_-]+$")

    private const val MIN_THROTTLING_LENGTH = 4
    private const val MAX_THROTTLING_LENGTH = 256

    fun verify(
        signatureProbes: List<YoutubeCipherProbe>,
        throttlingProbes: List<YoutubeCipherProbe>
    ): YoutubeConfigVerification {
        if (signatureProbes.isEmpty() && throttlingProbes.isEmpty()) {
            return YoutubeConfigVerification(YoutubeConfigVerdict.INCONCLUSIVE, "no probe evidence")
        }

        signatureProbes.forEach { probe ->
            val failure = signatureFailure(probe)
            if (failure != null) {
                return YoutubeConfigVerification(YoutubeConfigVerdict.REJECTED, failure)
            }
        }
        throttlingProbes.forEach { probe ->
            val failure = throttlingFailure(probe)
            if (failure != null) {
                return YoutubeConfigVerification(YoutubeConfigVerdict.REJECTED, failure)
            }
        }

        if (signatureProbes.size >= 2 && signatureProbes.map { it.output }.distinct().size == 1) {
            return YoutubeConfigVerification(YoutubeConfigVerdict.REJECTED, "signature transform collapses distinct inputs")
        }
        if (throttlingProbes.size >= 2 && throttlingProbes.map { it.output }.distinct().size == 1) {
            return YoutubeConfigVerification(YoutubeConfigVerdict.REJECTED, "n transform collapses distinct inputs")
        }

        if (signatureProbes.isEmpty() || throttlingProbes.isEmpty()) {
            return YoutubeConfigVerification(YoutubeConfigVerdict.INCONCLUSIVE, "partial probe coverage")
        }
        return YoutubeConfigVerification(YoutubeConfigVerdict.ACCEPTED, "cipher probes consistent")
    }

    private fun signatureFailure(probe: YoutubeCipherProbe): String? {
        val output = probe.output
        if (output.isBlank()) return "empty signature output"
        if (output == probe.input) return "signature output unchanged"
        if (output.length > probe.input.length) return "signature output longer than input"
        if (!signatureCharset.matches(output)) return "signature output charset invalid"
        val inputCharacters = probe.input.toSet()
        if (!output.all { it in inputCharacters }) return "signature output introduces foreign characters"
        return null
    }

    private fun throttlingFailure(probe: YoutubeCipherProbe): String? {
        val output = probe.output
        if (output.isBlank()) return "empty n output"
        if (output == probe.input) return "n output unchanged"
        if (output.length < MIN_THROTTLING_LENGTH) return "n output too short (${output.length})"
        if (output.length > MAX_THROTTLING_LENGTH) return "n output too long (${output.length})"
        if (!throttlingCharset.matches(output)) return "n output charset invalid"
        return null
    }
}

internal data class YoutubePlayerSample(
    val hash: String,
    val source: String
)

internal data class YoutubePlayerObservation(
    val dominantHash: String,
    val alternateHashes: List<String>
) {
    val rotating: Boolean
        get() = alternateHashes.isNotEmpty()

    val allHashes: List<String>
        get() = buildList {
            add(dominantHash)
            addAll(alternateHashes)
        }
}

internal object YoutubePlayerSampleAggregator {

    fun aggregate(samples: List<YoutubePlayerSample>): YoutubePlayerObservation? {
        val valid = samples
            .map { it.copy(hash = it.hash.trim().lowercase()) }
            .filter { YoutubePlayerConfigParser.isValidHash(it.hash) }
        if (valid.isEmpty()) return null

        val order = LinkedHashMap<String, Int>()
        valid.forEachIndexed { index, sample -> order.putIfAbsent(sample.hash, index) }
        val counts = valid.groupingBy { it.hash }.eachCount()

        val dominant = counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { order.getValue(it.key) }
            )
            .first()
            .key

        val alternates = order.keys.filterNot { it == dominant }
        return YoutubePlayerObservation(dominant, alternates)
    }
}
