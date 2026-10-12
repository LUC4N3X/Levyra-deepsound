package com.luc4n3x.levyra.data.youtubemusic

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class YoutubeMusicPaginationTest {
    @Test fun collectsAllPagesInOrderAndDeduplicatesIds() = runBlocking {
        val requested = mutableListOf<String>()
        val result = collectYoutubeMusicPages(key = { it: String -> it }) { token ->
            requested += token
            if (token.isEmpty()) YoutubeMusicPage(listOf("a", "b"), "test-next") else YoutubeMusicPage(listOf("b", "c"), "")
        }
        assertEquals(listOf("a", "b", "c"), result)
        assertEquals(listOf("", "test-next"), requested)
    }
    @Test fun repeatedContinuationIsAnErrorRatherThanPartialImport() = runBlocking {
        try {
            collectYoutubeMusicPages(key = { it: String -> it }) { YoutubeMusicPage(listOf("a"), "test-loop") }
            fail("Expected protocol error")
        } catch (_: YoutubeMusicProtocolException) { }
    }
    @Test fun oversizedLibraryIsNeverReportedAsComplete() = runBlocking {
        try {
            collectYoutubeMusicPages(maxItems = 1, key = { it: String -> it }) { YoutubeMusicPage(listOf("a", "b"), "") }
            fail("Expected limit error")
        } catch (_: YoutubeMusicLibraryLimit) { }
    }
    @Test fun pageCountIsBoundedEvenForEmptyPages() = runBlocking {
        var calls = 0
        try {
            collectYoutubeMusicPages(maxPages = 2, key = { it: String -> it }) { calls++; YoutubeMusicPage(emptyList(), "test-$calls") }
            fail("Expected limit error")
        } catch (_: YoutubeMusicLibraryLimit) { assertEquals(2, calls) }
    }
    @Test fun secondPageFailureNeverReturnsFirstPageAsSuccessfulImport() = runBlocking {
        try {
            collectYoutubeMusicPages(key = { it: String -> it }) { token ->
                if (token.isEmpty()) YoutubeMusicPage(listOf("a"), "test-next") else throw java.io.IOException("synthetic failure")
            }
            fail("Expected network error")
        } catch (_: java.io.IOException) { }
    }
    @Test fun cancellationPropagates() = runBlocking {
        try {
            collectYoutubeMusicPages(key = { it: String -> it }) { throw CancellationException("synthetic cancellation") }
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }
}
