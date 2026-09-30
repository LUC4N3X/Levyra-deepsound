package com.luc4n3x.levyra.viewmodel

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMarketSelectionContractTest {

    private fun source(relative: String): String {
        val path = sequenceOf(Path.of("app/$relative"), Path.of(relative)).firstOrNull(Files::exists)
            ?: error("$relative not found")
        return Files.readString(path)
    }

    private fun block(content: String, start: String, end: String): String {
        val from = content.indexOf(start)
        val to = content.indexOf(end, from + start.length)
        assertTrue("Missing block $start", from >= 0 && to > from)
        return content.substring(from, to)
    }

    @Test
    fun selectingAMarketPersistsItAndReloadsChartsExactlyOnce() {
        val viewModel = source("src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
        val selectChart = block(viewModel, "fun selectChart(regionId: String)", "private fun warmChartRegionMemoryCache")

        assertTrue(selectChart.contains("ChartsCatalog.supportedRegion(regionId)?.id ?: return"))
        assertTrue(selectChart.contains("preferences.setChartRegionId(normalizedRegionId)"))
        assertTrue(selectChart.contains("ChartsCatalog.requiresReload("))
        assertEquals(1, Regex("""\bloadCharts\(""").findAll(selectChart).count())
        assertTrue(selectChart.indexOf("setChartRegionId") < selectChart.indexOf("requiresReload"))
    }

    @Test
    fun changingLanguageSelectsThatLanguagesDefaultTop50Market() {
        val activity = source("src/main/java/com/luc4n3x/levyra/MainActivity.kt")

        assertTrue(activity.contains("var previousLanguageCode by remember { mutableStateOf(activityUiState.languageCode) }"))
        assertTrue(activity.contains("LaunchedEffect(activityUiState.languageCode)"))
        assertTrue(activity.contains("ChartsCatalog.defaultRegionForLanguage(currentLanguageCode).id"))
        assertTrue(activity.contains("viewModel.selectChart(defaultRegionId)"))
    }

    @Test
    fun interruptedSheetHideAllowsAnotherMarketSelection() {
        val sheet = source("src/main/java/com/luc4n3x/levyra/ui/ChartMarketSheet.kt")

        assertTrue(sheet.contains("if (sheetState.isVisible) committed = false else onDismiss()"))
    }

    @Test
    fun startupRestoresPersistedMarketBeforeDeviceRegion() {
        val viewModel = source("src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")

        assertTrue(viewModel.contains("storedRegionId = preferences.chartRegionId()"))
        assertTrue(viewModel.contains("deviceCountry = Locale.getDefault().country"))
        assertTrue(viewModel.contains("selectedChartId = startupChartRegion.id"))
        assertFalse(viewModel.contains("selectedChartId = ChartsCatalog.defaultRegionForLanguage("))
    }

    @Test
    fun homeUsesTheMarketSheetInsteadOfTheCountryPillRow() {
        val app = source("src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")

        assertFalse(app.contains("ChartRegionRow"))
        assertTrue(app.contains("ChartMarketSheet("))
        assertTrue(app.contains("onSelect = viewModel::selectChart"))
    }
}
