package com.luc4n3x.levyra.player

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.media3.session.CommandButton
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.FavoriteMembership
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.nextInCycle
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot

internal enum class SystemMediaCommand(val customAction: String) {
    AddFavorite("levyra.system.favorite.add"),
    RemoveFavorite("levyra.system.favorite.remove"),
    EnableShuffle("levyra.system.shuffle.enable"),
    DisableShuffle("levyra.system.shuffle.disable"),
    SetRepeatOff("levyra.system.repeat.off"),
    SetRepeatAll("levyra.system.repeat.all"),
    SetRepeatOne("levyra.system.repeat.one");

    companion object {
        fun fromCustomAction(customAction: String): SystemMediaCommand? =
            entries.firstOrNull { it.customAction == customAction }

        fun forRepeatMode(mode: RepeatMode): SystemMediaCommand = when (mode) {
            RepeatMode.Off -> SetRepeatOff
            RepeatMode.All -> SetRepeatAll
            RepeatMode.One -> SetRepeatOne
        }
    }
}

internal val SystemMediaCommand.repeatModeTarget: RepeatMode?
    get() = when (this) {
        SystemMediaCommand.SetRepeatOff -> RepeatMode.Off
        SystemMediaCommand.SetRepeatAll -> RepeatMode.All
        SystemMediaCommand.SetRepeatOne -> RepeatMode.One
        else -> null
    }

internal data class SystemMediaSessionItem(
    val mediaId: String,
    val liveRadio: Boolean
)

internal enum class SystemFavoriteState {
    Unavailable,
    NotFavorite,
    Favorite
}

internal data class SystemMediaActionState(
    val favorite: SystemFavoriteState,
    val shuffleEnabled: Boolean?,
    val repeatMode: RepeatMode?
)

internal data class SystemMediaButtonSpec(
    val command: SystemMediaCommand,
    val icon: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int
)

internal fun systemQueueControlsAvailable(
    queue: PlaybackQueueSnapshot,
    sessionItem: SystemMediaSessionItem?
): Boolean {
    val track = queue.currentTrack ?: return false
    return sessionItem != null && !sessionItem.liveRadio && !track.isLiveRadio()
}

internal fun systemFavoriteTarget(
    queue: PlaybackQueueSnapshot,
    sessionItem: SystemMediaSessionItem?
): Track? {
    if (!systemQueueControlsAvailable(queue, sessionItem)) return null
    val track = queue.currentTrack ?: return null
    return track.takeIf { LevyraMediaItemFactory.mediaId(it) == sessionItem?.mediaId }
}

internal fun systemMediaActionState(
    queue: PlaybackQueueSnapshot,
    sessionItem: SystemMediaSessionItem?,
    favorites: FavoriteMembership?
): SystemMediaActionState {
    if (!systemQueueControlsAvailable(queue, sessionItem)) {
        return SystemMediaActionState(SystemFavoriteState.Unavailable, shuffleEnabled = null, repeatMode = null)
    }
    val target = systemFavoriteTarget(queue, sessionItem)
    val favorite = when {
        target == null || favorites == null -> SystemFavoriteState.Unavailable
        favorites.contains(target) -> SystemFavoriteState.Favorite
        else -> SystemFavoriteState.NotFavorite
    }
    return SystemMediaActionState(favorite, queue.shuffleEnabled, queue.repeatMode)
}

internal fun systemMediaButtonSpecs(state: SystemMediaActionState): List<SystemMediaButtonSpec> = buildList {
    state.shuffleEnabled?.let { enabled -> add(shuffleButtonSpec(enabled)) }
    favoriteButtonSpec(state.favorite)?.let(::add)
    state.repeatMode?.let { mode -> add(repeatButtonSpec(mode)) }
}

private fun shuffleButtonSpec(enabled: Boolean): SystemMediaButtonSpec = if (enabled) {
    SystemMediaButtonSpec(
        SystemMediaCommand.DisableShuffle,
        CommandButton.ICON_SHUFFLE_ON,
        R.drawable.ic_notification_shuffle_on,
        R.string.notification_shuffle_disable
    )
} else {
    SystemMediaButtonSpec(
        SystemMediaCommand.EnableShuffle,
        CommandButton.ICON_SHUFFLE_OFF,
        R.drawable.ic_notification_shuffle,
        R.string.notification_shuffle_enable
    )
}

private fun favoriteButtonSpec(state: SystemFavoriteState): SystemMediaButtonSpec? = when (state) {
    SystemFavoriteState.Unavailable -> null
    SystemFavoriteState.NotFavorite -> SystemMediaButtonSpec(
        SystemMediaCommand.AddFavorite,
        CommandButton.ICON_HEART_UNFILLED,
        R.drawable.ic_notification_like_border,
        R.string.notification_favorite_add
    )
    SystemFavoriteState.Favorite -> SystemMediaButtonSpec(
        SystemMediaCommand.RemoveFavorite,
        CommandButton.ICON_HEART_FILLED,
        R.drawable.ic_notification_like,
        R.string.notification_favorite_remove
    )
}

private fun repeatButtonSpec(mode: RepeatMode): SystemMediaButtonSpec {
    val command = SystemMediaCommand.forRepeatMode(mode.nextInCycle())
    return when (mode) {
        RepeatMode.Off -> SystemMediaButtonSpec(
            command,
            CommandButton.ICON_REPEAT_OFF,
            R.drawable.ic_notification_repeat,
            R.string.notification_repeat_off
        )
        RepeatMode.All -> SystemMediaButtonSpec(
            command,
            CommandButton.ICON_REPEAT_ALL,
            R.drawable.ic_notification_repeat_on,
            R.string.notification_repeat_all
        )
        RepeatMode.One -> SystemMediaButtonSpec(
            command,
            CommandButton.ICON_REPEAT_ONE,
            R.drawable.ic_notification_repeat_one_on,
            R.string.notification_repeat_one
        )
    }
}
