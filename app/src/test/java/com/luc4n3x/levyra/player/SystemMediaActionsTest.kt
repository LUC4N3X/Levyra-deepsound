package com.luc4n3x.levyra.player

import androidx.media3.session.CommandButton
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.FavoriteMembership
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.nextInCycle
import com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemMediaActionsTest {
    private val first = track("first", "First")
    private val second = track("second", "Second")

    @Test
    fun unlikedCurrentTrackExposesAddFavoriteAction() {
        val state = stateFor(queue(first), favorites = FavoriteMembership.of(listOf(second)))

        assertEquals(SystemFavoriteState.NotFavorite, state.favorite)
        val spec = specFor(state, SystemMediaCommand.AddFavorite)
        assertEquals(CommandButton.ICON_HEART_UNFILLED, spec.icon)
        assertEquals(R.drawable.ic_notification_like_border, spec.iconRes)
        assertEquals(R.string.notification_favorite_add, spec.labelRes)
    }

    @Test
    fun likedCurrentTrackExposesRemoveFavoriteAction() {
        val state = stateFor(queue(first), favorites = FavoriteMembership.of(listOf(first)))

        assertEquals(SystemFavoriteState.Favorite, state.favorite)
        val spec = specFor(state, SystemMediaCommand.RemoveFavorite)
        assertEquals(CommandButton.ICON_HEART_FILLED, spec.icon)
        assertEquals(R.drawable.ic_notification_like, spec.iconRes)
        assertEquals(R.string.notification_favorite_remove, spec.labelRes)
    }

    @Test
    fun shuffleOffExposesEnableShuffleAction() {
        val state = stateFor(queue(first, shuffle = false))

        assertEquals(false, state.shuffleEnabled)
        val spec = specFor(state, SystemMediaCommand.EnableShuffle)
        assertEquals(CommandButton.ICON_SHUFFLE_OFF, spec.icon)
        assertEquals(R.drawable.ic_notification_shuffle, spec.iconRes)
        assertEquals(R.string.notification_shuffle_enable, spec.labelRes)
    }

    @Test
    fun shuffleOnExposesDisableShuffleAction() {
        val state = stateFor(queue(first, shuffle = true))

        assertEquals(true, state.shuffleEnabled)
        val spec = specFor(state, SystemMediaCommand.DisableShuffle)
        assertEquals(CommandButton.ICON_SHUFFLE_ON, spec.icon)
        assertEquals(R.drawable.ic_notification_shuffle_on, spec.iconRes)
        assertEquals(R.string.notification_shuffle_disable, spec.labelRes)
    }

    @Test
    fun repeatOffShowsOffStateAndTargetsRepeatAll() {
        val spec = repeatSpec(RepeatMode.Off)

        assertEquals(SystemMediaCommand.SetRepeatAll, spec.command)
        assertEquals(CommandButton.ICON_REPEAT_OFF, spec.icon)
        assertEquals(R.drawable.ic_notification_repeat, spec.iconRes)
        assertEquals(R.string.notification_repeat_off, spec.labelRes)
    }

    @Test
    fun repeatAllShowsAllStateAndTargetsRepeatOne() {
        val spec = repeatSpec(RepeatMode.All)

        assertEquals(SystemMediaCommand.SetRepeatOne, spec.command)
        assertEquals(CommandButton.ICON_REPEAT_ALL, spec.icon)
        assertEquals(R.drawable.ic_notification_repeat_on, spec.iconRes)
        assertEquals(R.string.notification_repeat_all, spec.labelRes)
    }

    @Test
    fun repeatOneShowsOneStateAndTargetsRepeatOff() {
        val spec = repeatSpec(RepeatMode.One)

        assertEquals(SystemMediaCommand.SetRepeatOff, spec.command)
        assertEquals(CommandButton.ICON_REPEAT_ONE, spec.icon)
        assertEquals(R.drawable.ic_notification_repeat_one_on, spec.iconRes)
        assertEquals(R.string.notification_repeat_one, spec.labelRes)
    }

    @Test
    fun repeatCycleWrapsOffAllOneOff() {
        assertEquals(RepeatMode.All, RepeatMode.Off.nextInCycle())
        assertEquals(RepeatMode.One, RepeatMode.All.nextInCycle())
        assertEquals(RepeatMode.Off, RepeatMode.One.nextInCycle())

        var mode = RepeatMode.Off
        val visited = mutableListOf(mode)
        repeat(3) {
            mode = repeatSpec(mode).command.repeatModeTarget ?: error("Repeat button without repeat target")
            visited += mode
        }
        assertEquals(listOf(RepeatMode.Off, RepeatMode.All, RepeatMode.One, RepeatMode.Off), visited)
    }

    @Test
    fun trackChangeRefreshesFavoriteStateForNewCurrentTrack() {
        val favorites = FavoriteMembership.of(listOf(first))
        val before = queue(first, second)
        val after = before.copy(currentIndex = 1)

        assertEquals(SystemFavoriteState.Favorite, stateFor(before, favorites = favorites).favorite)
        assertEquals(SystemFavoriteState.NotFavorite, stateFor(after, favorites = favorites).favorite)
    }

    @Test
    fun restoredSessionProducesRestoredActionsInStableOrder() {
        val restored = queue(first, second, currentIndex = 1, shuffle = true, repeat = RepeatMode.One)

        val specs = systemMediaButtonSpecs(
            systemMediaActionState(restored, sessionItemFor(second), FavoriteMembership.of(listOf(second)))
        )

        assertEquals(
            listOf(SystemMediaCommand.DisableShuffle, SystemMediaCommand.RemoveFavorite, SystemMediaCommand.SetRepeatOff),
            specs.map { it.command }
        )
    }

    @Test
    fun sessionWithoutItemYetPublishesNoActions() {
        val restored = queue(first, shuffle = true, repeat = RepeatMode.All)

        val state = systemMediaActionState(restored, sessionItem = null, favorites = FavoriteMembership.of(listOf(first)))

        assertEquals(SystemMediaActionState(SystemFavoriteState.Unavailable, null, null), state)
        assertTrue(systemMediaButtonSpecs(state).isEmpty())
    }

    @Test
    fun favoriteTargetIsTheLogicalCurrentQueueTrack() {
        val snapshot = queue(first, second, currentIndex = 1)

        assertSame(second, systemFavoriteTarget(snapshot, sessionItemFor(second)))
    }

    @Test
    fun favoriteIsNotExposedWhileSessionStillShowsThePreviousTrack() {
        val snapshot = queue(first, second, currentIndex = 1)
        val stalePrevious = sessionItemFor(first)

        assertNull(systemFavoriteTarget(snapshot, stalePrevious))
        val state = stateFor(snapshot, sessionItem = stalePrevious, favorites = FavoriteMembership.of(listOf(first)))
        assertEquals(SystemFavoriteState.Unavailable, state.favorite)
        assertEquals(
            listOf(SystemMediaCommand.EnableShuffle, SystemMediaCommand.SetRepeatAll),
            systemMediaButtonSpecs(state).map { it.command }
        )
    }

    @Test
    fun stateMappingDoesNotMutateQueue() {
        val snapshot = queue(first, second, currentIndex = 1, shuffle = true, repeat = RepeatMode.All)
        val copy = snapshot.copy(tracks = snapshot.tracks.toList(), shuffleOrder = snapshot.shuffleOrder.toList())

        systemMediaButtonSpecs(stateFor(snapshot, favorites = FavoriteMembership.of(listOf(second))))
        systemFavoriteTarget(snapshot, sessionItemFor(second))

        assertEquals(copy, snapshot)
    }

    @Test
    fun emptyQueueExposesNoActions() {
        val state = systemMediaActionState(PlaybackQueueSnapshot(), sessionItemFor(first), FavoriteMembership.of(listOf(first)))

        assertEquals(SystemMediaActionState(SystemFavoriteState.Unavailable, null, null), state)
        assertTrue(systemMediaButtonSpecs(state).isEmpty())
        assertNull(systemFavoriteTarget(PlaybackQueueSnapshot(), sessionItemFor(first)))
    }

    @Test
    fun liveRadioExposesNoActions() {
        val radio = track("live-radio:station", "Station", source = "Live Radio")
        val snapshot = queue(radio)

        assertTrue(systemMediaButtonSpecs(stateFor(snapshot)).isEmpty())
        assertTrue(
            systemMediaButtonSpecs(
                systemMediaActionState(queue(first), SystemMediaSessionItem("first", liveRadio = true), FavoriteMembership.of(emptyList()))
            ).isEmpty()
        )
        assertNull(systemFavoriteTarget(snapshot, sessionItemFor(radio)))
    }

    @Test
    fun unknownFavoriteStateHidesOnlyTheFavoriteAction() {
        val state = stateFor(queue(first, repeat = RepeatMode.All), favorites = null)

        assertEquals(SystemFavoriteState.Unavailable, state.favorite)
        assertEquals(
            listOf(SystemMediaCommand.EnableShuffle, SystemMediaCommand.SetRepeatOne),
            systemMediaButtonSpecs(state).map { it.command }
        )
    }

    @Test
    fun customActionsRoundTripAndUnknownActionsAreIgnored() {
        SystemMediaCommand.entries.forEach { command ->
            assertEquals(command, SystemMediaCommand.fromCustomAction(command.customAction))
        }
        assertEquals(SystemMediaCommand.entries.size, SystemMediaCommand.entries.map { it.customAction }.toSet().size)
        assertNull(SystemMediaCommand.fromCustomAction("levyra.favorite.like"))
        assertNull(SystemMediaCommand.fromCustomAction("levyra.queue.shuffle"))
    }

    private fun stateFor(
        snapshot: PlaybackQueueSnapshot,
        sessionItem: SystemMediaSessionItem? = snapshot.currentTrack?.let(::sessionItemFor),
        favorites: FavoriteMembership? = FavoriteMembership.of(emptyList())
    ): SystemMediaActionState = systemMediaActionState(snapshot, sessionItem, favorites)

    private fun specFor(state: SystemMediaActionState, command: SystemMediaCommand): SystemMediaButtonSpec =
        systemMediaButtonSpecs(state).single { it.command == command }

    private fun repeatSpec(mode: RepeatMode): SystemMediaButtonSpec =
        systemMediaButtonSpecs(stateFor(queue(first, repeat = mode))).single { it.command.repeatModeTarget != null }

    private fun sessionItemFor(track: Track) =
        SystemMediaSessionItem(LevyraMediaItemFactory.mediaId(track), liveRadio = track.source == "Live Radio")

    private fun queue(
        vararg tracks: Track,
        currentIndex: Int = 0,
        shuffle: Boolean = false,
        repeat: RepeatMode = RepeatMode.Off
    ) = PlaybackQueueSnapshot(
        tracks = tracks.toList(),
        currentIndex = currentIndex,
        shuffleEnabled = shuffle,
        shuffleOrder = if (shuffle) tracks.indices.toList() else emptyList(),
        repeatMode = repeat
    )

    private fun track(id: String, title: String, source: String = "test") = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = source,
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
