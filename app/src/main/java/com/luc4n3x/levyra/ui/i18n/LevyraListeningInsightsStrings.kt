package com.luc4n3x.levyra.ui.i18n

private fun listeningInsightsStrings(
    title: String,
    subtitle: String,
    period24h: String,
    period7d: String,
    period30d: String,
    period6m: String,
    periodAll: String,
    listened: String,
    previous: String,
    activity: String,
    rhythm: String,
    activeAround: String,
    discovery: String,
    newTracks: String,
    history: String,
    searchHistory: String,
    today: String,
    yesterday: String,
    loadMore: String,
    detailAvailable: String,
    error: String,
    empty: String
): Map<String, String> = mapOf(
    "listeningInsights" to title,
    "listeningInsightsSubtitle" to subtitle,
    "insightsPeriod24h" to period24h,
    "insightsPeriod7d" to period7d,
    "insightsPeriod30d" to period30d,
    "insightsPeriod6m" to period6m,
    "insightsPeriodAll" to periodAll,
    "insightsListened" to listened,
    "insightsPrevious" to previous,
    "insightsActivity" to activity,
    "insightsRhythm" to rhythm,
    "insightsActiveAround" to activeAround,
    "insightsDiscovery" to discovery,
    "insightsNewTracks" to newTracks,
    "insightsHistory" to history,
    "insightsSearchHistory" to searchHistory,
    "insightsToday" to today,
    "insightsYesterday" to yesterday,
    "insightsLoadMore" to loadMore,
    "insightsDetailAvailable" to detailAvailable,
    "insightsError" to error,
    "insightsEmpty" to empty
)

private val listeningInsightsEnglish = listeningInsightsStrings(
    title = "Listening Insights",
    subtitle = "The shape of your listening",
    period24h = "24H",
    period7d = "7D",
    period30d = "30D",
    period6m = "6M",
    periodAll = "ALL",
    listened = "LISTENED",
    previous = "vs previous period",
    activity = "Listening activity",
    rhythm = "Your rhythm",
    activeAround = "Most active around %s",
    discovery = "Discovery",
    newTracks = "%s new tracks",
    history = "History",
    searchHistory = "Search your history",
    today = "Today",
    yesterday = "Yesterday",
    loadMore = "Load more",
    detailAvailable = "Detailed activity available from %s",
    error = "Insights could not be loaded",
    empty = "Your listening story starts with the next song"
)

private val listeningInsightsItalian = listeningInsightsStrings(
    title = "Listening Insights",
    subtitle = "La forma dei tuoi ascolti",
    period24h = "24H",
    period7d = "7G",
    period30d = "30G",
    period6m = "6M",
    periodAll = "TUTTO",
    listened = "ASCOLTATI",
    previous = "rispetto al periodo precedente",
    activity = "Attività di ascolto",
    rhythm = "Il tuo ritmo",
    activeAround = "Più attivo intorno alle %s",
    discovery = "Scoperte",
    newTracks = "%s nuovi brani",
    history = "Cronologia",
    searchHistory = "Cerca nella cronologia",
    today = "Oggi",
    yesterday = "Ieri",
    loadMore = "Carica altro",
    detailAvailable = "Attività dettagliata disponibile dal %s",
    error = "Impossibile caricare gli insight",
    empty = "La storia dei tuoi ascolti inizia dal prossimo brano"
)

internal val listeningInsightsKeys: Set<String> = listeningInsightsEnglish.keys

internal fun listeningInsightsLocalizationEntries(code: String): Map<String, String> =
    if (code == "it") listeningInsightsItalian else listeningInsightsEnglish
