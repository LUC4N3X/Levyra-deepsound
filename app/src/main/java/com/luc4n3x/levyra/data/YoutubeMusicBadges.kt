package com.luc4n3x.levyra.data

internal fun String.hasExplicitBadge(): Boolean =
    contains("MUSIC_EXPLICIT_BADGE") || contains("MUSIC_ITEM_BADGE_EXPLICIT")
