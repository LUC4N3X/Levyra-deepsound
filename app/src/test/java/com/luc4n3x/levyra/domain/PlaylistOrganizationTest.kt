package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistOrganizationTest {

    @Test
    fun normalizationFoldsCaseAccentsAndWhitespace() {
        assertEquals("relax", normalizePlaylistTagName("  Relax  "))
        assertEquals("relax", normalizePlaylistTagName("RELAX"))
        assertEquals("cafe", normalizePlaylistTagName("Café"))
        assertEquals("gym time", normalizePlaylistTagName("Gym   Time"))
        assertEquals("", normalizePlaylistTagName("   "))
    }

    @Test
    fun sanitizationTrimsAndBoundsLength() {
        val long = "a".repeat(PLAYLIST_TAG_MAX_LENGTH + 20)

        assertEquals(PLAYLIST_TAG_MAX_LENGTH, sanitizePlaylistTagName(long).length)
        assertEquals("Gym Time", sanitizePlaylistTagName(" Gym   Time "))
    }

    @Test
    fun blankTagNamesAreRejected() {
        assertFalse(isValidPlaylistTagName("   "))
        assertTrue(isValidPlaylistTagName("Rap"))
    }

    @Test
    fun visibilityHelpersSplitLibraryAndHiddenPlaylists() {
        val visible = playlist("visible")
        val hidden = playlist("hidden", hidden = true)
        val playlists = listOf(visible, hidden)

        assertEquals(listOf("visible"), playlists.visibleInLibrary().map { it.id })
        assertEquals(listOf("hidden"), playlists.hiddenInLibrary().map { it.id })
    }

    @Test
    fun emptyTagSelectionKeepsTheWholeList() {
        val playlists = listOf(playlist("a"), playlist("b"))

        assertSame(playlists, playlists.filterByTagIds(emptySet()))
    }

    @Test
    fun tagFilterRequiresEverySelectedTag() {
        val gym = tag("gym")
        val rap = tag("rap")
        val both = playlist("both", tags = listOf(gym, rap))
        val onlyGym = playlist("onlyGym", tags = listOf(gym))

        val result = listOf(both, onlyGym).filterByTagIds(setOf(gym.id, rap.id))

        assertEquals(listOf("both"), result.map { it.id })
    }

    @Test
    fun tagsInUseAreDeduplicatedAndOrdered() {
        val gym = tag("gym")
        val auto = tag("auto")
        val playlists = listOf(
            playlist("a", tags = listOf(gym)),
            playlist("b", tags = listOf(gym, auto))
        )

        assertEquals(listOf("auto", "gym"), playlistTagsInUse(playlists).map { it.normalizedName })
    }

    @Test
    fun selectionTogglesAndStopsAtTheLimit() {
        val gym = tag("gym")

        val added = mergePlaylistTagSelection(emptyList(), gym)
        assertEquals(listOf(gym.id), added.map { it.id })

        val removed = mergePlaylistTagSelection(added, gym)
        assertTrue(removed.isEmpty())

        val full = (1..PLAYLIST_TAG_MAX_PER_PLAYLIST).map { tag("tag$it") }
        val overflow = mergePlaylistTagSelection(full, tag("extra"))
        assertEquals(PLAYLIST_TAG_MAX_PER_PLAYLIST, overflow.size)
    }

    private fun tag(name: String) = PlaylistTag(
        id = "id-$name",
        name = name.replaceFirstChar(Char::uppercase),
        normalizedName = normalizePlaylistTagName(name),
        createdAt = 0L
    )

    private fun playlist(
        id: String,
        tags: List<PlaylistTag> = emptyList(),
        hidden: Boolean = false
    ) = Playlist(
        id = id,
        name = id,
        coverUrl = "",
        tracks = emptyList(),
        createdAt = 0L,
        updatedAt = 0L,
        tags = tags,
        hidden = hidden
    )
}
