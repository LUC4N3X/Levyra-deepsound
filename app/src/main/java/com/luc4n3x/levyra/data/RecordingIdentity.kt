package com.luc4n3x.levyra.data

internal enum class RecordingIdentityMatch {
    Exact,
    Conflict,
    Unknown
}

internal fun normalizedIsrc(value: String): String = value
    .uppercase()
    .filter(Char::isLetterOrDigit)
    .takeIf { it.matches(Regex("[A-Z]{2}[A-Z0-9]{3}[0-9]{7}")) }
    .orEmpty()

internal fun recordingIdentityMatch(reference: String, candidate: String): RecordingIdentityMatch {
    val expected = normalizedIsrc(reference)
    val actual = normalizedIsrc(candidate)
    if (expected.isBlank() || actual.isBlank()) return RecordingIdentityMatch.Unknown
    return if (expected == actual) RecordingIdentityMatch.Exact else RecordingIdentityMatch.Conflict
}
