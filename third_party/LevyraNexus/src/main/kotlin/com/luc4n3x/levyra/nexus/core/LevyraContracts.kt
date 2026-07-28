package com.luc4n3x.levyra.nexus.core

data class LevyraRequestContext(
    val languageCode: String = "en",
    val countryCode: String = "US",
    val offlineAllowed: Boolean = true,
    val deadlineMs: Long = 30_000L,
    val attributes: Map<String, String> = emptyMap()
)

fun interface LevyraCatalogGateway<Q, R> {
    suspend fun execute(query: Q, context: LevyraRequestContext): LevyraResult<R>
}

fun interface LevyraStreamResolver<I, O> {
    suspend fun resolve(input: I, context: LevyraRequestContext): LevyraResult<O>
}

fun interface LevyraLyricsGateway<I, O> {
    suspend fun load(input: I, context: LevyraRequestContext): LevyraResult<O>
}

interface LevyraQueueStore<T> {
    suspend fun load(): LevyraResult<List<T>>
    suspend fun save(items: List<T>): LevyraResult<Unit>
}

fun interface LevyraRecommendationEngine<T, C> {
    suspend fun rank(candidates: List<T>, context: C): LevyraResult<List<T>>
}

fun interface LevyraPlaybackHistoryStore<E> {
    suspend fun append(event: E): LevyraResult<Unit>
}

interface LevyraPlaybackPort<T> {
    suspend fun prepare(item: T): LevyraResult<Unit>
    suspend fun play(): LevyraResult<Unit>
    suspend fun pause(): LevyraResult<Unit>
    suspend fun seekTo(positionMs: Long): LevyraResult<Unit>
    suspend fun stop(): LevyraResult<Unit>
}
