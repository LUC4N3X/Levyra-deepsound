package com.luc4n3x.levyra.feature.radio

import android.content.SharedPreferences
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class RadioStoreTest {
    @Test
    fun customStationSurvivesStoreRecreationAndEditsEverySavedReference() = runBlocking {
        val preferences = MemoryRadioPreferences()
        val store = RadioStore(preferences)
        val station = customRadioStation("Радио Маяк", "http://relay.example:8000/live", "https://radio.example/logo.png")
        store.saveCustom(station)
        store.setFavorite(station, true, 100)
        store.recordRecent(station, 1234)
        store.savePlaybackStation(station)
        val restored = RadioStore(preferences)
        assertEquals(station, restored.customStations().single())
        val edited = customRadioStation("Маяк HD", "https://relay.example/stream.m3u8", "", station)
        restored.saveCustom(edited)
        assertEquals(station.uuid, edited.uuid)
        assertEquals(edited, RadioStore(preferences).customStations().single())
        assertEquals(edited.copy(addedAt = 100), restored.favorites().single())
        assertEquals(edited.copy(lastPlayedAt = 1234), restored.recent().single())
        assertEquals(edited, restored.playbackStation())
    }

    @Test
    fun deletionRemovesCustomStationFromFavoritesRecentAndRestoration() = runBlocking {
        val store = RadioStore(MemoryRadioPreferences())
        val station = customRadioStation("Radio", "https://radio.example/live", "")
        store.saveCustom(station)
        store.setFavorite(station, true, 100)
        store.recordRecent(station, 1)
        store.savePlaybackStation(station)
        store.deleteCustom(station)
        assertTrue(store.customStations().isEmpty())
        assertTrue(store.favorites().isEmpty())
        assertTrue(store.recent().isEmpty())
        assertNull(store.playbackStation())
    }

    @Test
    fun canonicalDuplicateIsRejectedAndFavoriteTogglingDoesNotDuplicate() = runBlocking {
        val preferences = MemoryRadioPreferences()
        val store = RadioStore(preferences)
        val station = customRadioStation("A", "https://Radio.example:443/live#now", "")
        store.saveCustom(station)
        try {
            RadioStore(preferences).saveCustom(customRadioStation("B", "https://radio.example/live", ""))
            fail("duplicate accepted")
        } catch (error: CustomRadioException) { assertEquals(CustomRadioError.Duplicate, error.reason) }
        store.setFavorite(station, true, 100)
        store.setFavorite(station, true, 100)
        assertEquals(1, store.favorites().size)
        store.setFavorite(station, false, 200)
        assertTrue(store.favorites().isEmpty())
        assertEquals(1, store.customStations().size)
    }

    @Test
    fun favoriteFlowSeesChangesFromAnotherStoreInstance() = runBlocking {
        val preferences = MemoryRadioPreferences()
        val firstEmission = CompletableDeferred<Unit>()
        val observed = async {
            withTimeout(5000) { RadioStore(preferences).observeFavorites().onEach { firstEmission.complete(Unit) }.take(2).toList() }
        }
        firstEmission.await()
        val station = customRadioStation("Radio", "https://radio.example/live", "")
        RadioStore(preferences).apply { saveCustom(station); setFavorite(station, true, 100) }
        assertEquals(listOf(emptyList<RadioStation>(), listOf(station.copy(addedAt = 100))), observed.await())
    }

    @Test
    fun storageFailureIsReported() = runBlocking {
        val preferences = MemoryRadioPreferences().apply { commitsSucceed = false }
        try {
            RadioStore(preferences).saveCustom(customRadioStation("Radio", "https://radio.example/live", ""))
            fail("save failure swallowed")
        } catch (error: CustomRadioException) { assertEquals(CustomRadioError.Storage, error.reason) }
    }

    @Test
    fun stalePlaybackCallbacksCannotResurrectDeletedOrEditedCustomStations() = runBlocking {
        val store = RadioStore(MemoryRadioPreferences())
        val original = customRadioStation("Before", "https://radio.example/old", "")
        store.saveCustom(original)
        val updated = customRadioStation("After", "https://radio.example/new", "", original)
        store.saveCustom(updated)
        store.setFavorite(original, true, 100)
        store.recordRecent(original, 1)
        store.savePlaybackStation(original)
        assertEquals(updated.copy(addedAt = 100), store.favorites().single())
        assertEquals(updated, store.playbackStation())
        store.deleteCustom(updated)
        store.setFavorite(original, true, 100)
        store.recordRecent(original, 2)
        store.savePlaybackStation(original)
        assertTrue(store.favorites().isEmpty())
        assertTrue(store.recent().isEmpty())
        assertNull(store.playbackStation())
    }
    @Test
    fun favoritesRecordWhenTheyWereAddedNewestFirst() = runBlocking {
        val store = RadioStore(MemoryRadioPreferences())
        val first = customRadioStation("First", "https://radio.example/first", "")
        val second = customRadioStation("Second", "https://radio.example/second", "")
        store.saveCustom(first)
        store.saveCustom(second)
        store.setFavorite(first, true, 1_000)
        store.setFavorite(second, true, 2_000)
        assertEquals(listOf(second.copy(addedAt = 2_000), first.copy(addedAt = 1_000)), store.favorites())
        store.setFavorite(first, true, 3_000)
        assertEquals(listOf(first.uuid to 3_000L, second.uuid to 2_000L), store.favorites().map { it.uuid to it.addedAt })
    }

    @Test
    fun editingCustomStationKeepsItsFavoriteAddedTime() = runBlocking {
        val store = RadioStore(MemoryRadioPreferences())
        val station = customRadioStation("Before", "https://radio.example/live", "")
        store.saveCustom(station)
        store.setFavorite(station, true, 4_200)
        store.saveCustom(customRadioStation("After", "https://radio.example/live2", "", station))
        val favorite = store.favorites().single()
        assertEquals("After", favorite.name)
        assertEquals(4_200L, favorite.addedAt)
    }

    @Test
    fun legacyFavoritesWithoutAddedTimeKeepTheirSavedOrder() = runBlocking {
        val preferences = MemoryRadioPreferences()
        val legacy = (1..3).joinToString(prefix = "[", postfix = "]") { index ->
            """{"uuid":"legacy-$index","name":"Legacy $index","url":"https://radio.example/$index","tags":["rock"],"playedAt":0}"""
        }
        preferences.edit().putString("favorites", legacy).commit()
        val store = RadioStore(preferences)
        val restored = store.favorites()
        assertEquals(listOf("legacy-1", "legacy-2", "legacy-3"), restored.map { it.uuid })
        assertTrue(restored.all { it.addedAt == 0L })
        val added = customRadioStation("Fresh", "https://radio.example/fresh", "")
        store.saveCustom(added)
        store.setFavorite(added, true, 9_000)
        val favorites = store.favorites()
        val labels = RadioFavoriteGroupLabels({ "" }, { it.name }, "Unknown", "Other")
        val newest = radioFavoriteGroups(favorites, RadioFavoriteSort.Newest, Locale.ENGLISH, labels, ::facts).single().items
        val oldest = radioFavoriteGroups(favorites, RadioFavoriteSort.Oldest, Locale.ENGLISH, labels, ::facts).single().items
        assertEquals(listOf(added.uuid, "legacy-1", "legacy-2", "legacy-3"), newest.map { it.uuid })
        assertEquals(listOf("legacy-3", "legacy-2", "legacy-1", added.uuid), oldest.map { it.uuid })
        assertEquals(favorites, newest)
    }

    @Test
    fun favoriteSortSurvivesStoreRecreationAndUnknownValuesFallBack() = runBlocking {
        val preferences = MemoryRadioPreferences()
        assertEquals(RadioFavoriteSort.Newest, RadioStore(preferences).observeFavoriteSort().first())
        RadioStore(preferences).setFavoriteSort(RadioFavoriteSort.Genre)
        assertEquals(RadioFavoriteSort.Genre, RadioStore(preferences).observeFavoriteSort().first())
        preferences.edit().putString("favorite_sort", "removed_option").commit()
        assertEquals(RadioFavoriteSort.Default, RadioStore(preferences).observeFavoriteSort().first())
    }

    @Test
    fun favoriteSortFlowSeesChangesFromAnotherStoreInstance() = runBlocking {
        val preferences = MemoryRadioPreferences()
        val firstEmission = CompletableDeferred<Unit>()
        val observed = async {
            withTimeout(5000) { RadioStore(preferences).observeFavoriteSort().onEach { firstEmission.complete(Unit) }.take(2).toList() }
        }
        firstEmission.await()
        RadioStore(preferences).setFavoriteSort(RadioFavoriteSort.NameDescending)
        assertEquals(listOf(RadioFavoriteSort.Newest, RadioFavoriteSort.NameDescending), observed.await())
    }

    private fun facts(station: RadioStation) =
        RadioFavoriteFacts(station.name, station.countryCode, station.country, station.tags, station.addedAt)
}

internal class MemoryRadioPreferences : SharedPreferences {
    private val values = ConcurrentHashMap<String, Any>()
    private val listeners = java.util.concurrent.CopyOnWriteArraySet<SharedPreferences.OnSharedPreferenceChangeListener>()
    var commitsSucceed = true
    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun contains(key: String?) = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { listener?.let(listeners::add) }
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) { listeners.remove(listener) }
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        val changes = linkedMapOf<String, Any?>()
        var clear = false
        override fun putString(key: String?, value: String?) = apply { changes[key!!] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { changes[key!!] = values }
        override fun putInt(key: String?, value: Int) = apply { changes[key!!] = value }
        override fun putLong(key: String?, value: Long) = apply { changes[key!!] = value }
        override fun putFloat(key: String?, value: Float) = apply { changes[key!!] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { changes[key!!] = value }
        override fun remove(key: String?) = apply { changes[key!!] = null }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean {
            if (!commitsSucceed) return false
            if (clear) values.clear()
            changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
            changes.keys.forEach { key -> listeners.forEach { it.onSharedPreferenceChanged(this@MemoryRadioPreferences, key) } }
            return true
        }
        override fun apply() { commit() }
    }
}
