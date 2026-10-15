package com.luc4n3x.levyra.domain

object LevyraYtMusicPairing {
    fun miniStyleFor(previous: NavStyle, next: NavStyle, current: MiniPlayerStyle): MiniPlayerStyle = when {
        next == NavStyle.YtMusic -> MiniPlayerStyle.YtMusic
        previous == NavStyle.YtMusic && current == MiniPlayerStyle.YtMusic -> MiniPlayerStyle.Original
        else -> current
    }

    fun navStyleFor(previous: MiniPlayerStyle, next: MiniPlayerStyle, current: NavStyle): NavStyle = when {
        next == MiniPlayerStyle.YtMusic -> NavStyle.YtMusic
        previous == MiniPlayerStyle.YtMusic && current == NavStyle.YtMusic -> NavStyle.Original
        else -> current
    }
}
