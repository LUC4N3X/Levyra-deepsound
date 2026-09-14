package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.AutoEqCatalogSource
import com.luc4n3x.levyra.domain.AutoEqCatalog
import com.luc4n3x.levyra.domain.AutoEqCatalogEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoEqCatalogControllerTest {
    private val catalog = AutoEqCatalog.parseIndex(
        """
        - [Sony WH-1000XM4](./Rtings/HMS%20II.3%20over-ear/Sony%20WH-1000XM4) by Rtings on HMS II.3
        - [Sennheiser HD 600](./oratory1990/over-ear/Sennheiser%20HD%20600) by oratory1990
        """.trimIndent()
    )

    @Test
    fun openShowsLoadingThenPublishesSearchResults() {
        val source = FakeSource()
        val controller = controller(source)

        controller.open()
        assertTrue(controller.state.value.visible)
        assertEquals(AutoEqCatalogStatus.LOADING, controller.state.value.status)

        source.catalogResult.complete(catalog)
        controller.updateQuery("sony")

        assertEquals(AutoEqCatalogStatus.READY, controller.state.value.status)
        assertEquals(listOf("Sony WH-1000XM4"), controller.state.value.results.map { it.name })
        controller.updateQuery(" ")
        assertTrue(controller.state.value.results.isEmpty())
    }

    @Test
    fun unavailableCatalogCanBeRetried() {
        val source = FakeSource().apply { catalogResult.complete(null) }
        val controller = controller(source)

        controller.open()
        assertEquals(AutoEqCatalogStatus.UNAVAILABLE, controller.state.value.status)

        source.catalogResult = CompletableDeferred(catalog)
        controller.open()

        assertEquals(AutoEqCatalogStatus.READY, controller.state.value.status)
        assertEquals(2, source.catalogRequests)
    }

    @Test
    fun onlyLatestProfileSelectionIsPublished() {
        val (controller, source) = readyController()
        val (first, second) = controller.state.value.results

        controller.select(first)
        controller.select(second)
        source.profile(first).complete(PROFILE)
        assertNull(controller.state.value.selection)
        assertEquals(second.key, controller.state.value.loadingKey)

        source.profile(second).complete(PROFILE)

        assertEquals(second.name, controller.state.value.selection?.name)
        assertEquals(PROFILE, controller.state.value.selection?.profileText)
        assertNull(controller.state.value.loadingKey)
    }

    @Test
    fun failedProfileDownloadIsReportedWithoutSelection() {
        val (controller, source) = readyController()
        val entry = controller.state.value.results.first()

        controller.select(entry)
        source.profile(entry).complete(null)

        assertEquals(entry.key, controller.state.value.failedKey)
        assertNull(controller.state.value.selection)
    }

    @Test
    fun dismissingSelectionKeepsTheSearchSession() {
        val (controller, source) = readyController()
        val entry = controller.state.value.results.first()
        controller.select(entry)
        source.profile(entry).complete(PROFILE)

        controller.dismissSelection()

        assertNull(controller.state.value.selection)
        assertEquals("s", controller.state.value.query)
        assertEquals(2, controller.state.value.results.size)
    }

    @Test
    fun closeReleasesMemoryAndDropsLateResults() {
        val source = FakeSource()
        val controller = controller(source)
        controller.open()

        controller.close()
        source.catalogResult.complete(catalog)

        assertEquals(AutoEqCatalogUiState(), controller.state.value)
        assertEquals(1, source.released)
    }

    private fun readyController(): Pair<AutoEqCatalogController, FakeSource> {
        val source = FakeSource().apply { catalogResult.complete(catalog) }
        val controller = controller(source)
        controller.open()
        controller.updateQuery("s")
        return controller to source
    }

    private fun controller(source: FakeSource) = AutoEqCatalogController(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        source = source,
        searchDispatcher = Dispatchers.Unconfined,
        searchDebounceMs = 0L
    )

    private class FakeSource : AutoEqCatalogSource {
        var catalogResult = CompletableDeferred<AutoEqCatalog?>()
        var catalogRequests = 0
        var released = 0
        private val profiles = mutableMapOf<String, CompletableDeferred<String?>>()

        fun profile(entry: AutoEqCatalogEntry): CompletableDeferred<String?> =
            profiles.getOrPut(entry.key) { CompletableDeferred() }

        override suspend fun loadCatalog(): AutoEqCatalog? {
            catalogRequests++
            return catalogResult.await()
        }

        override suspend fun loadProfile(entry: AutoEqCatalogEntry): String? = profile(entry).await()

        override fun releaseMemory() {
            released++
        }
    }

    private companion object {
        const val PROFILE = "GraphicEQ: 20 -1.0; 1000 0.0; 20000 1.0"
    }
}
