package com.luc4n3x.levyra.ui.i18n

internal val playerGestureKeys = setOf(
    "playerGestureHorizontalSwipe",
    "playerGestureHorizontalSwipeSubtitle",
    "playerGestureDoubleTapAction",
    "playerGestureDoubleTapActionSubtitle",
    "playerGestureLongPressAction",
    "playerGestureLongPressActionSubtitle",
    "playerGestureVerticalSwipe",
    "playerGestureVerticalSwipeSubtitle",
    "gestureActionSeek",
    "gestureActionPlayPause",
    "gestureActionFavorite",
    "gestureActionQueue",
    "gestureActionLyrics",
    "gestureActionSpeed",
    "gestureActionBrightnessVolume",
    "gestureActionVolume",
    "gestureActionDisabled"
)

private fun playerGestures(
    horizontalSwipe: String,
    horizontalSwipeSubtitle: String,
    doubleTap: String,
    doubleTapSubtitle: String,
    longPress: String,
    longPressSubtitle: String,
    verticalSwipe: String,
    verticalSwipeSubtitle: String,
    seek: String,
    playPause: String,
    favorite: String,
    queue: String,
    lyrics: String,
    speed: String,
    brightnessVolume: String,
    volume: String,
    disabled: String
): Map<String, String> = mapOf(
    "playerGestureHorizontalSwipe" to horizontalSwipe,
    "playerGestureHorizontalSwipeSubtitle" to horizontalSwipeSubtitle,
    "playerGestureDoubleTapAction" to doubleTap,
    "playerGestureDoubleTapActionSubtitle" to doubleTapSubtitle,
    "playerGestureLongPressAction" to longPress,
    "playerGestureLongPressActionSubtitle" to longPressSubtitle,
    "playerGestureVerticalSwipe" to verticalSwipe,
    "playerGestureVerticalSwipeSubtitle" to verticalSwipeSubtitle,
    "gestureActionSeek" to seek,
    "gestureActionPlayPause" to playPause,
    "gestureActionFavorite" to favorite,
    "gestureActionQueue" to queue,
    "gestureActionLyrics" to lyrics,
    "gestureActionSpeed" to speed,
    "gestureActionBrightnessVolume" to brightnessVolume,
    "gestureActionVolume" to volume,
    "gestureActionDisabled" to disabled
)

private val playerGestureBundles = mapOf(
    "en" to playerGestures(
        "Swipe artwork",
        "Move to the previous or next track",
        "Double tap",
        "Choose what a double tap does",
        "Long press",
        "Choose what holding the artwork does",
        "Vertical swipe",
        "Controls available along the artwork edges",
        "Seek",
        "Play or pause",
        "Favorite",
        "Open queue",
        "Open lyrics",
        "Temporary speed",
        "Brightness and volume",
        "Volume",
        "Off"
    ),
    "it" to playerGestures(
        "Scorri sulla copertina",
        "Passa alla traccia precedente o successiva",
        "Doppio tap",
        "Scegli l'azione del doppio tap",
        "Pressione prolungata",
        "Scegli l'azione quando tieni premuta la copertina",
        "Scorrimento verticale",
        "Controlli disponibili lungo i bordi della copertina",
        "Avanzamento rapido",
        "Riproduci o metti in pausa",
        "Preferito",
        "Apri coda",
        "Apri testo",
        "Velocità temporanea",
        "Luminosità e volume",
        "Volume",
        "Disattivato"
    )
)

internal fun playerGestureLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(playerGestureBundles, code)
