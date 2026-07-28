package com.luc4n3x.levyra.nexus.core

enum class LevyraDataSource {
    NETWORK,
    CACHE,
    FALLBACK,
    LOCAL
}

enum class LevyraFreshness {
    LIVE,
    FRESH,
    STALE
}

enum class LevyraFailureType {
    NETWORK,
    TIMEOUT,
    RATE_LIMITED,
    ACCESS_DENIED,
    NOT_FOUND,
    INVALID_DATA,
    CANCELLED,
    SECURITY,
    UNSUPPORTED,
    UNKNOWN
}

sealed interface LevyraResult<out T> {
    data class Success<T>(
        val value: T,
        val source: LevyraDataSource = LevyraDataSource.NETWORK,
        val freshness: LevyraFreshness = LevyraFreshness.LIVE,
        val metadata: Map<String, String> = emptyMap()
    ) : LevyraResult<T>

    data class Failure(
        val type: LevyraFailureType,
        val retryable: Boolean,
        val message: String = "",
        val cause: Throwable? = null,
        val metadata: Map<String, String> = emptyMap()
    ) : LevyraResult<Nothing>
}

inline fun <T, R> LevyraResult<T>.map(transform: (T) -> R): LevyraResult<R> = when (this) {
    is LevyraResult.Success -> LevyraResult.Success(
        value = transform(value),
        source = source,
        freshness = freshness,
        metadata = metadata
    )
    is LevyraResult.Failure -> this
}

inline fun <T, R> LevyraResult<T>.fold(
    onSuccess: (LevyraResult.Success<T>) -> R,
    onFailure: (LevyraResult.Failure) -> R
): R = when (this) {
    is LevyraResult.Success -> onSuccess(this)
    is LevyraResult.Failure -> onFailure(this)
}
