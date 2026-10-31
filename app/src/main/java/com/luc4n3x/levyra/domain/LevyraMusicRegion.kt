package com.luc4n3x.levyra.domain

object LevyraMusicRegion {
    const val AUTO = ""

    @Volatile
    var selectedId: String = AUTO
        private set

    fun apply(regionId: String?) {
        selectedId = normalize(regionId)
    }

    fun normalize(regionId: String?): String =
        ChartsCatalog.supportedRegion(regionId.orEmpty())?.id ?: AUTO

    fun cacheScope(languageCode: String, regionId: String = selectedId): String {
        val language = LevyraLanguageCatalog.normalize(languageCode)
        val region = normalize(regionId)
        return if (region == AUTO || LevyraContentLocales.catalogLocale(language).chartRegionId == region) {
            language
        } else {
            "${language}_$region"
        }
    }
}
