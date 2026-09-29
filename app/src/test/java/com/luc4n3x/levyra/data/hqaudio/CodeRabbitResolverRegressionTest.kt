package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.HighQualityAudioMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeRabbitResolverRegressionTest {
    @Test
    fun transientFailureBeforeDefinitiveMissIsNotNegativeCached() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val transient = FakeHighQualityProvider(
                searchOutcome = { ProviderSearchOutcome.Failed(ProviderFailure.NETWORK) },
                providerId = "transient-provider",
                requiresHighQualityMode = false
            )
            val definitiveMiss = FakeHighQualityProvider(
                searchOutcome = { ProviderSearchOutcome.Found(emptyList()) },
                providerId = "definitive-miss-provider",
                requiresHighQualityMode = false
            )
            val resolver = HighQualityAudioResolver(
                providers = listOf(transient, definitiveMiss),
                mappingStore = HighQualityMappingStore(InMemoryMappingStorage()),
                scope = scope
            ).apply {
                mode = HighQualityAudioMode.AUTOMATIC
            }
            val identity = "coderabbit-transient-before-miss"

            val first = resolver.await(resolver.begin(identity, query()), 5_000L)
            val second = resolver.await(resolver.begin(identity, query()), 5_000L)

            assertEquals(HighQualityFallbackReason.NO_MATCH, (first as HighQualityResolution.Fallback).reason)
            assertEquals(HighQualityFallbackReason.NO_MATCH, (second as HighQualityResolution.Fallback).reason)
            assertEquals(2, transient.searches.size)
            assertEquals(2, definitiveMiss.searches.size)
        } finally {
            scope.cancel()
        }
    }
}
