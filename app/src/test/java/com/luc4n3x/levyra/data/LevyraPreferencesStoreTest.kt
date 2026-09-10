package com.luc4n3x.levyra.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.luc4n3x.levyra.domain.LevyraAudioPreset
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraAutomationSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPreferencesStoreTest {
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val persisted = MutableStateFlow(emptyPreferences())

        @Volatile
        var readGate: CompletableDeferred<Unit>? = null

        override val data: Flow<Preferences> = flow {
            readGate?.await()
            emitAll(persisted)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(persisted.value).also { persisted.value = it } }
    }

    private val scopes = mutableListOf<CoroutineScope>()
    private val disk = InMemoryPreferencesDataStore()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun open(): Pair<LevyraPreferencesStore, LevyraPreferences> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        val store = LevyraPreferencesStore(disk, scope)
        return store to LevyraPreferences(store)
    }

    private fun reopen(): LevyraPreferences = open().second

    private fun flush(store: LevyraPreferencesStore) = runBlocking { store.commit { } }

    @Test
    fun emptyStoreExposesExistingDefaults() {
        val (_, preferences) = open()
        val snapshot = preferences.snapshot()

        assertFalse(snapshot.onboarded)
        assertTrue(snapshot.animationsEnabled)
        assertTrue(snapshot.dynamicColor)
        assertEquals(DEFAULT_SPONSORBLOCK_ENABLED, snapshot.sponsorBlock)
        assertEquals("Auto", snapshot.audioQuality)
        assertEquals(LevyraAudioSettings().normalized(), preferences.audioSettings())
        assertEquals(PlayerVisualMode.Artwork, preferences.interfaceSettings().playerVisualMode)
        assertNull(snapshot.lastTrack)
        assertEquals(LevyraAutomationSettings().normalized(), runBlocking { preferences.automationSettingsFlow.first() })
    }

    @Test
    fun persistedValuesAndLegacyMappingsLoadIntoMemory() {
        runBlocking {
            disk.edit {
                it[booleanPreferencesKey("onboarded")] = true
                it[stringPreferencesKey("user_name")] = "Luca"
                it[intPreferencesKey("audio_crossfade_seconds")] = 6
                it[booleanPreferencesKey("motion_artwork_enabled")] = true
            }
        }
        val (_, preferences) = open()

        assertTrue(preferences.isOnboarded())
        assertEquals("Luca", preferences.userName())
        assertEquals(6, preferences.audioSettings().crossfadeSeconds)
        assertEquals(PlayerVisualMode.CanvasCard, preferences.interfaceSettings().playerVisualMode)
    }

    @Test
    fun writesAreVisibleImmediatelyAndSurviveRecreation() {
        val (store, preferences) = open()

        preferences.setUserName("Luca")
        preferences.setLanguageCode("it")
        preferences.setDynamicColor(false)
        preferences.setSkipSilence(true)
        preferences.setJamDisplayName("  DJ Luca  ")
        assertEquals("Luca", preferences.userName())
        assertEquals("it", preferences.languageCode())
        assertFalse(preferences.dynamicColor())
        assertTrue(preferences.skipSilence())
        assertEquals("DJ Luca", preferences.jamDisplayName())

        flush(store)
        val reopened = reopen()

        assertEquals("Luca", reopened.userName())
        assertEquals("it", reopened.languageCode())
        assertFalse(reopened.dynamicColor())
        assertTrue(reopened.skipSilence())
        assertEquals("DJ Luca", reopened.jamDisplayName())
    }

    @Test
    fun rapidWritesKeepLastIntent() {
        val (store, preferences) = open()

        repeat(200) { index ->
            preferences.setAudioSettings(LevyraAudioSettings(crossfadeSeconds = index % 12))
        }
        preferences.setAudioSettings(LevyraAudioSettings(crossfadeSeconds = 3, playbackSpeed = 1.25f, pitch = 0.9f))
        assertEquals(3, preferences.audioSettings().crossfadeSeconds)

        flush(store)
        val reopened = reopen().audioSettings()

        assertEquals(3, reopened.crossfadeSeconds)
        assertEquals(1.25f, reopened.playbackSpeed, 0f)
        assertEquals(0.9f, reopened.pitch, 0f)
    }

    @Test
    fun concurrentWritesToDifferentKeysAreAllKept() {
        val (store, preferences) = open()

        runBlocking {
            (0 until 50).map { index ->
                async(Dispatchers.Default) {
                    if (index % 2 == 0) preferences.setUserName("user-$index") else preferences.setAudioQuality("High")
                    preferences.setListeningLifetimeBackfillVersion(7)
                }
            }.awaitAll()
        }
        preferences.setUserName("final")
        flush(store)

        val reopened = reopen()
        assertEquals("final", reopened.userName())
        assertEquals("High", reopened.audioQuality())
        assertEquals(7, reopened.listeningLifetimeBackfillVersion())
    }

    @Test
    fun writesIssuedBeforeInitialLoadAreAppliedOverPersistedState() {
        runBlocking {
            disk.edit {
                it[stringPreferencesKey("user_name")] = "Persisted"
                it[booleanPreferencesKey("onboarded")] = true
            }
        }
        val gate = CompletableDeferred<Unit>()
        disk.readGate = gate
        val (store, preferences) = open()

        preferences.setUserName("Newer")
        gate.complete(Unit)

        assertEquals("Newer", preferences.userName())
        assertTrue(preferences.isOnboarded())
        flush(store)
        disk.readGate = null
        val reopened = reopen()
        assertEquals("Newer", reopened.userName())
        assertTrue(reopened.isOnboarded())
    }

    @Test
    fun restoreSnapshotIsVisibleImmediatelyAndDurable() {
        val (_, preferences) = open()
        val customPreset = LevyraAudioPreset(
            id = "${LevyraAudioPresets.CUSTOM_PRESET_PREFIX}night",
            fallbackLabel = "Night",
            levels = List(LevyraAudioPresets.bandCount) { it - 2 },
            bassBoost = 60,
            virtualizer = 40,
            preampDb = -2f
        )
        val restored = preferences.snapshot().copy(
            onboarded = true,
            userName = "Restored",
            languageCode = "de",
            audioSettings = LevyraAudioSettings(
                equalizerEnabled = true,
                presetId = customPreset.id,
                bandLevels = customPreset.levels,
                customPresets = listOf(customPreset),
                crossfadeSeconds = 5
            ),
            interfaceSettings = LevyraInterfaceSettings(playerVisualMode = PlayerVisualMode.CanvasCard),
            automationSettings = LevyraAutomationSettings(pauseOnMute = true),
            jamDisplayName = "Jam"
        )
        preferences.setUserName("Before restore")

        runBlocking { preferences.restoreSnapshot(restored) }

        assertEquals("Restored", preferences.userName())
        assertEquals("de", preferences.languageCode())
        assertEquals(listOf(customPreset), preferences.audioSettings().customPresets)
        assertTrue(runBlocking { preferences.automationSettingsFlow.first() }.pauseOnMute)

        val snapshot = reopen().snapshot()
        assertTrue(snapshot.onboarded)
        assertEquals("Restored", snapshot.userName)
        assertEquals("de", snapshot.languageCode)
        assertEquals(customPreset.id, snapshot.audioSettings.presetId)
        assertEquals(listOf(customPreset), snapshot.audioSettings.customPresets)
        assertEquals(5, snapshot.audioSettings.crossfadeSeconds)
        assertEquals(PlayerVisualMode.CanvasCard, snapshot.interfaceSettings.playerVisualMode)
        assertTrue(snapshot.automationSettings.pauseOnMute)
        assertEquals("Jam", snapshot.jamDisplayName)
    }

    @Test
    fun structuredValuesAreParsedOncePerStateAndRefreshAfterWrites() {
        val (store, preferences) = open()
        val track = Track(
            id = "abc",
            title = "Song",
            artist = "Artist",
            album = "",
            durationMs = 1000L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "youtube",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0
        )
        preferences.saveRecentSearches(listOf(track))
        preferences.saveLastPlayback(track, 4200L)

        val first = preferences.snapshot()
        assertSame(first, preferences.snapshot())
        assertEquals(listOf("abc"), preferences.loadRecentSearches().map { it.id })
        assertEquals("abc", preferences.lastTrack()?.id)
        assertEquals(4200L, preferences.lastPositionMs())

        flush(store)
        val reopened = reopen()
        assertEquals("abc", reopened.lastTrack()?.id)
        assertEquals(4200L, reopened.lastPositionMs())

        preferences.saveLastPlayback(null, 0L)
        assertNull(preferences.lastTrack())
        assertEquals(0L, preferences.lastPositionMs())
    }

    @Test
    fun automationFlowFollowsInMemoryWrites() {
        val (_, preferences) = open()

        runBlocking { preferences.setAutomationSettings(LevyraAutomationSettings(resumeOnBluetoothReconnect = true)) }

        assertTrue(runBlocking { preferences.automationSettingsFlow.first() }.resumeOnBluetoothReconnect)
    }
}
