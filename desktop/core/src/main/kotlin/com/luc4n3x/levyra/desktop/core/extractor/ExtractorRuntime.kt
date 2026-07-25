package com.luc4n3x.levyra.desktop.core.extractor

import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import java.util.concurrent.atomic.AtomicReference
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

object ExtractorRuntime {
    private val current = AtomicReference<String>(null)
    private val downloader by lazy { DesktopDownloader() }

    @Synchronized
    fun ensureInitialized(language: AppLanguage, countryCode: String) {
        val country = countryCode.trim().uppercase().take(2).ifBlank { "IT" }
        val signature = "${language.tag}-$country"
        if (current.get() == signature) return
        NewPipe.init(
            downloader,
            Localization(language.tag, country),
            ContentCountry(country)
        )
        current.set(signature)
    }

    fun localization(): Localization = NewPipe.getPreferredLocalization()

    fun contentCountry(): ContentCountry = NewPipe.getPreferredContentCountry()
}
