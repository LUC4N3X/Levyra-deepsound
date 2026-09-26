package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.domain.LevyraContentLocale
import com.luc4n3x.levyra.domain.LevyraContentLocales

object YoutubeRegionProfile {
    const val US_GL = "US"
    const val US_HL = "en"
    const val US_ACCEPT_LANGUAGE = "en-US,en;q=0.9"

    fun isEnabled(): Boolean {
        return LevyraNetworkConfiguration.current().youtubeRegionProfileEnabled
    }

    fun effectiveLocale(preferredLanguageCode: String): LevyraContentLocale {
        if (isEnabled()) {
            return LevyraContentLocales.forLanguage("en")
        }
        return LevyraContentLocales.forLanguage(preferredLanguageCode)
    }

    fun effectiveAcceptLanguage(defaultAcceptLanguage: String): String {
        if (isEnabled()) {
            return US_ACCEPT_LANGUAGE
        }
        return defaultAcceptLanguage
    }
}
