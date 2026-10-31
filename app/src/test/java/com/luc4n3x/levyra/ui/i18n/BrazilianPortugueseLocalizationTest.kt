package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraMusicRegion
import com.luc4n3x.levyra.domain.MoodEngine
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrazilianPortugueseLocalizationTest {
    private val placeholderPattern = Regex("""\{\w+\}|%(\d+\$)?[,.\d]*[sdf]""")
    private val europeanTerms = Regex(
        """(?iu)(?<![\p{L}-])(ecrã|ecrãs|ficheiro|ficheiros|definições|transferir|transferências?|telemóvel|gerir|guardar|guardad[oa]s?|eliminar|utilizador|palavra-passe|a reproduzir|a carregar|a transferir|teu|tua|teus|tuas|leitor|partilhar|controlos?)(?![\p{L}-])"""
    )

    @After
    fun resetRegion() {
        LevyraMusicRegion.apply(LevyraMusicRegion.AUTO)
    }

    @Test
    fun tagVariantsResolveToTheRightPortuguese() {
        listOf("pt-BR", "pt_BR", "pt-rBR", "PT-br", "pt_br").forEach { tag ->
            assertEquals(tag, "pt-BR", LevyraLanguageCatalog.normalize(tag))
        }
        listOf("pt", "pt-PT", "pt_PT", "pt-rPT", "pt-AO").forEach { tag ->
            assertEquals(tag, "pt", LevyraLanguageCatalog.normalize(tag))
        }
        assertEquals("pt", LevyraLanguageCatalog.translationCode("pt-BR"))
        assertEquals("zh-Hant", LevyraLanguageCatalog.normalize("zh-Hant"))
        assertEquals("id", LevyraLanguageCatalog.normalize("in"))
    }

    @Test
    fun catalogListsBothPortugueseVariantsWithFlags() {
        val options = LevyraLanguageCatalog.selectableLanguages.filter { it.code.startsWith("pt") }
        assertEquals(listOf("pt", "pt-BR"), options.map { it.code })
        assertEquals("🇵🇹 Português (Portugal)", LevyraLanguageCatalog.displayName("pt"))
        assertEquals("🇧🇷 Português (Brasil)", LevyraLanguageCatalog.displayName("pt-BR"))
        assertFalse(LevyraLanguageCatalog.languages.any { it.code == "pt-BR" })
        assertFalse(LevyraLanguageCatalog.isRtl("pt-BR"))
    }

    @Test
    fun brazilAndPortugalResolveDifferentContent() {
        val brazil = LevyraContentLocales.forLanguage("pt-BR")
        val portugal = LevyraContentLocales.forLanguage("pt")

        assertEquals("pt-BR", brazil.hl)
        assertEquals("BR", brazil.gl)
        assertEquals("br", brazil.chartRegionId)
        assertEquals("pt-PT", portugal.hl)
        assertEquals("PT", portugal.gl)
        assertEquals("br", ChartsCatalog.defaultRegionForLanguage("pt-BR").id)
        assertEquals("pt", ChartsCatalog.defaultRegionForLanguage("pt").id)
        assertTrue(brazil.homeQueries.all { !it.contains("Portugal") })
        assertTrue(LevyraContentLocales.artistSuggestions("pt-BR").contains("Anitta"))
        assertFalse(LevyraContentLocales.artistSuggestions("pt").contains("Anitta"))
        assertNotEquals(LevyraContentLocales.quickSearches("pt"), LevyraContentLocales.quickSearches("pt-BR"))
        assertEquals("Brasil", MoodEngine().moodsForLanguage("pt-BR").first { it.id == "italia" }.title)
        assertEquals("Portugal", MoodEngine().moodsForLanguage("pt").first { it.id == "italia" }.title)
    }

    @Test
    fun musicRegionIsIndependentFromUiLanguage() {
        val italian = LevyraContentLocales.catalogLocale("it")
        val italianInBrazil = LevyraContentLocales.withMusicRegion(italian, "br")
        assertEquals("it", italianInBrazil.languageCode)
        assertEquals("it", italianInBrazil.hl)
        assertEquals("BR", italianInBrazil.gl)
        assertEquals("pt-BR", italianInBrazil.contentLanguageCode)
        assertEquals(italian.quickSectionTitle, italianInBrazil.quickSectionTitle)
        assertEquals(LevyraContentLocales.catalogLocale("pt-BR").homeQueries, italianInBrazil.homeQueries)

        val english = LevyraContentLocales.withMusicRegion(LevyraContentLocales.catalogLocale("en"), "br")
        assertEquals("en", english.hl)
        assertEquals("BR", english.gl)

        assertEquals(
            LevyraContentLocales.catalogLocale("pt-BR"),
            LevyraContentLocales.withMusicRegion(LevyraContentLocales.catalogLocale("pt-BR"), "br")
        )
        assertEquals(
            LevyraContentLocales.catalogLocale("pt"),
            LevyraContentLocales.withMusicRegion(LevyraContentLocales.catalogLocale("pt"), "pt")
        )
        assertEquals("GB", LevyraContentLocales.withMusicRegion(italian, "gb").gl)
        assertEquals("en", LevyraContentLocales.withMusicRegion(italian, "gb").contentLanguageCode)
        assertEquals("es", LevyraContentLocales.withMusicRegion(italian, "mx").contentLanguageCode)
    }

    @Test
    fun localWaveFollowsTheSelectedMusicRegion() {
        val italianStrings = LevyraStrings.forCode("it")
        fun localWave() = com.luc4n3x.levyra.domain.ExploreCatalog.getZones(italianStrings)
            .first { it.id == com.luc4n3x.levyra.domain.ExploreCatalog.LOCAL_WAVE_ZONE_ID }
        LevyraMusicRegion.apply("br")
        assertEquals("Brasil Wave", localWave().label)
        assertEquals(LevyraContentLocales.catalogLocale("pt-BR").homeQueries.first(), localWave().query)
        LevyraMusicRegion.apply(LevyraMusicRegion.AUTO)
        assertEquals(italianStrings.localWaveName, localWave().label)
    }

    @Test
    fun selectedRegionDrivesContentAndCacheScope() {
        assertEquals("it", LevyraMusicRegion.cacheScope("it"))
        LevyraMusicRegion.apply("br")

        assertEquals("BR", LevyraContentLocales.forLanguage("it").gl)
        assertEquals("it", LevyraContentLocales.forLanguage("it").hl)
        assertEquals("br", ChartsCatalog.defaultRegionForLanguage("it").id)
        assertTrue(LevyraContentLocales.quickSearches("it").contains("Anitta"))
        assertTrue(LevyraContentLocales.artistSuggestions("en").contains("Anitta"))
        assertEquals("it_br", LevyraMusicRegion.cacheScope("it"))
        assertEquals("pt-BR", LevyraMusicRegion.cacheScope("pt-BR"))
        assertEquals("br", ChartsCatalog.startupRegion("", "IT", "it").id)
        assertEquals("Brasil", MoodEngine().moodsForLanguage("it").first { it.id == "italia" }.title)
        assertEquals("Esplora artisti", LevyraContentLocales.artistSuggestionsTitle("it"))

        LevyraMusicRegion.apply("not-a-region")
        assertEquals(LevyraMusicRegion.AUTO, LevyraMusicRegion.selectedId)
    }

    @Test
    fun chartRegionFollowsLanguageOnlyWhileItWasTheLanguageDefault() {
        assertEquals("br", ChartsCatalog.regionAfterLanguageChange("", "pt", "pt", "pt-BR").id)
        assertEquals("it", ChartsCatalog.regionAfterLanguageChange("", "it", "pt", "pt-BR").id)
        assertEquals("pt", ChartsCatalog.regionAfterLanguageChange("pt", "pt", "pt", "pt-BR").id)
    }

    @Test
    fun brazilianBundleKeepsEveryKeyAndPlaceholder() {
        val portugal = LevyraStrings.forCode("pt")
        val brazil = LevyraStrings.forCode("pt-BR")
        assertEquals("pt-BR", brazil.code)
        assertEquals("pt", portugal.code)
        assertEquals(portugal.entryKeys, brazil.entryKeys)
        portugal.entryKeys.forEach { key ->
            assertEquals(
                key,
                placeholderPattern.findAll(portugal.value(key)).map { it.value }.toList().sorted(),
                placeholderPattern.findAll(brazil.value(key)).map { it.value }.toList().sorted()
            )
        }
    }

    @Test
    fun brazilianBundleNeverFallsToEnglishWhereTheBaseIsTranslated() {
        val english = LevyraStrings.forCode("en")
        val portugal = LevyraStrings.forCode("pt")
        val brazil = LevyraStrings.forCode("pt-BR")
        val intentionalLoanwords = setOf("Timer", "Download", "Downloads", "Player", "Backup", "Backups", "Login", "Tags")
        portugal.entryKeys
            .filter { portugal.value(it) != english.value(it) && brazil.value(it) !in intentionalLoanwords }
            .forEach { key -> assertNotEquals(key, english.value(key), brazil.value(key)) }
    }

    @Test
    fun importMessagesNeverRewriteThePlaylistName() {
        val name = "O ecrã a tocar"
        assertTrue(playlistImportSuccessMessage("pt-BR", 3, name).endsWith(name))
        assertTrue(playlistImportSuccessMessage("pt-BR", 2, 3, name).endsWith(name))
    }

    @Test
    fun remainingCopyUsesBrazilianAddressForms() {
        assertEquals(
            "Nocturne 78 controla o aspecto. Mude para Levyra Default para personalizá-lo. As escolhas anteriores ficam salvas.",
            LevyraSkinStrings.managedForCode("pt-BR")
        )
        assertTrue(LevyraSkinStrings.managedForCode("pt-BR", "Prism").contains("Prism"))
        assertEquals("Descobertas para você", LevyraBrazilianPortuguese.adapt("Descobertas para si"))
    }

    @Test
    fun brazilianBundleOverridesRegionalTerms() {
        val brazil = LevyraStrings.forCode("pt-BR")
        val leftovers = brazil.entryKeys.mapNotNull { key ->
            europeanTerms.find(brazil.value(key))?.let { "$key: ${brazil.value(key)}" }
        }
        assertEquals(emptyList<String>(), leftovers)
        assertEquals("Configurações", brazil.settings)
        assertEquals("Baixar", brazil.download)
        assertEquals("Salvar", brazil.save)
        assertEquals("Excluir", brazil.delete)
        assertEquals("Eletrônica", brazil.exploreElectronic)
        assertEquals("Eletrónica", LevyraStrings.forCode("pt").exploreElectronic)
        assertEquals("Minha trilha sonora", brazil.value("homeSoundtrackTitle"))
        assertEquals("A minha banda sonora", LevyraStrings.forCode("pt").value("homeSoundtrackTitle"))
        assertEquals("Manter a tela ligada", brazil.keepScreenOn)
        assertEquals("Impede que a tela se desligue automaticamente durante a reprodução de música.", brazil.keepScreenOnSubtitle)
        assertEquals("Salvo para ouvir offline", brazil.offlineSavedForListening)
        assertEquals("Espaço de armazenamento insuficiente. Libere espaço e tente novamente.", brazil.offlineStorageFull)
    }

    @Test
    fun typedCatalogsResolveBrazilianTextWithoutEnglishOrCrashes() {
        assertEquals("Remover esta música dos seus downloads?", LevyraDownloadRemovalStrings.prompt("pt-BR"))
        assertEquals("Compartilhar", LevyraYouTubeMusicSkinStrings.forCode("pt-BR").share)
        assertNotEquals(LevyraSkinStrings.forCode("en").defaultBody, LevyraSkinStrings.forCode("pt-BR").defaultBody)
        assertFalse(europeanTerms.containsMatchIn(LevyraSkinStrings.forCode("pt-BR").defaultBody))
        assertFalse(europeanTerms.containsMatchIn(deviceIntegrationCopy("pt-BR").toString()))
        assertFalse(europeanTerms.containsMatchIn(LevyraOnboardingYoutubeMusicStrings.forCode("pt-BR").toString()))
        assertFalse(europeanTerms.containsMatchIn(LevyraQobuzSkinStrings.forCode("pt-BR").toString()))
        assertNotEquals(discoveryCopy("en").swipeHint, discoveryCopy("pt-BR").swipeHint)
        assertNotEquals(radioLibraryCopy("en").toString(), "")
        assertEquals(
            LevyraPersonalCollectionCatalog.forCode("pt").code,
            LevyraPersonalCollectionCatalog.forCode("pt-BR").code
        )
        assertNotEquals(LevyraLiveRadioCatalog.forCode("en").exploreSubtitle, LevyraLiveRadioCatalog.forCode("pt-BR").exploreSubtitle)
        assertNotEquals(musicRegionCopy("en").title, musicRegionCopy("pt-BR").title)
        assertEquals("Região musical", musicRegionCopy("pt-BR").title)
    }

    @Test
    fun portugalStringsStayEuropean() {
        val portugal = LevyraStrings.forCode("pt")
        assertEquals("Definições", portugal.settings)
        assertEquals("Transferir", portugal.download)
        assertEquals("Guardar", portugal.save)
    }

    @Test
    fun catalogLookupsAlwaysGoThroughTheTranslationKey() {
        val forbidden = listOf(
            Regex("""\w+\[code] \?: \w+\.getValue\("en"\)"""),
            Regex("""when \(code\)"""),
            Regex("""\[LevyraLanguageCatalog\.normalize\(code\)]"""),
            Regex("""getValue\(LevyraLanguageCatalog\.normalize\(code\)\)""")
        )
        val violations = File("src/main/java/com/luc4n3x/levyra/ui/i18n").listFiles().orEmpty()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (forbidden.any { it.containsMatchIn(line) }) "${file.name}:${index + 1}: ${line.trim()}" else null
                }
            }
        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun imperativeFormsOnlyApplyAtSentenceStart() {
        assertEquals("Adicione músicas", LevyraBrazilianPortuguese.adaptDynamic("Adiciona músicas"))
        assertEquals("Pronto. Abra a fila", LevyraBrazilianPortuguese.adaptDynamic("Pronto. Abra a fila"))
        assertEquals("Pronto. Abra a fila", LevyraBrazilianPortuguese.adaptDynamic("Pronto. Abre a fila"))
        assertEquals("Linha\nToque aqui", LevyraBrazilianPortuguese.adaptDynamic("Linha\nToca aqui"))
        assertEquals("O modo Adiciona faixas", LevyraBrazilianPortuguese.adaptDynamic("O modo Adiciona faixas"))
        assertEquals("Cada lista Usa o Levyra", LevyraBrazilianPortuguese.adaptDynamic("Cada lista Usa o Levyra"))
        assertEquals("Versão 2.5 Ativa", LevyraBrazilianPortuguese.adaptDynamic("Versão 2.5 Ativa"))
        val brazil = LevyraStrings.forCode("pt-BR")
        assertTrue(brazil.value("restoreBackupSubtitle").startsWith("Verifica "))
        assertTrue(brazil.value("automaticTranslationSubtitle").startsWith("Usa "))
        assertTrue(brazil.value("playerGestureHorizontalSwipeSubtitle").startsWith("Muda "))
        assertFalse(brazil.value("downloadTrackHint").contains("para ouvindo"))
    }

    @Test
    fun brazilianAdaptationOfTheWholeBundleIsCheap() {
        val portugal = LevyraStrings.forCode("pt")
        val values = portugal.entryKeys.map(portugal::value)
        val coldStarted = System.nanoTime()
        values.forEach(LevyraBrazilianPortuguese::adaptDynamic)
        val coldMs = (System.nanoTime() - coldStarted) / 1_000_000
        val started = System.nanoTime()
        values.forEach(LevyraBrazilianPortuguese::adaptDynamic)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        println("pt-BR bundle adaptation: ${values.size} strings, first pass ${coldMs}ms, warm ${elapsedMs}ms")
        assertTrue("pt-BR first adaptation took ${coldMs}ms", coldMs < 500)
        assertTrue("pt-BR adaptation took ${elapsedMs}ms", elapsedMs < 250)
    }

    @Test
    fun backupWithoutMusicRegionKeepsTheCurrentRegion() {
        LevyraMusicRegion.apply("br")
        assertEquals("br", com.luc4n3x.levyra.data.backupMusicRegionId(org.json.JSONObject()))
        assertEquals("", com.luc4n3x.levyra.data.backupMusicRegionId(org.json.JSONObject().put("musicRegionId", "")))
        assertEquals("pt", com.luc4n3x.levyra.data.backupMusicRegionId(org.json.JSONObject().put("musicRegionId", "pt")))
        assertEquals("", com.luc4n3x.levyra.data.backupMusicRegionId(org.json.JSONObject().put("musicRegionId", "zz")))
    }

    @Test
    fun homeSnapshotScopeIsFixedByTheRegionCapturedAtScheduling() {
        LevyraMusicRegion.apply("br")
        assertEquals("it", LevyraMusicRegion.cacheScope("it", ""))
        assertEquals("it_br", LevyraMusicRegion.cacheScope("it", "br"))
        val source = File("src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt").readText()
        val persist = source.substringAfter("private fun persistHomeSnapshot()").substringBefore("@Synchronized")
        assertTrue(persist.contains("val musicRegionId = LevyraMusicRegion.selectedId"))
        assertTrue(persist.contains("LevyraMusicRegion.selectedId != musicRegionId) return"))
        assertTrue(persist.contains("musicRegionId = musicRegionId"))
    }

    @Test
    fun restoreRefreshesTheVisibleMusicRegion() {
        val source = File("src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt").readText()
        val restore = source.substringAfter("private suspend fun refreshAfterRestore()").substringBefore("applyLanguageContent(snapshot.languageCode")
        assertTrue(restore.contains("_musicRegionId.value = snapshot.musicRegionId"))
        assertTrue(restore.contains("NewPipeRuntime.onConfigurationChanged()"))
    }
}
