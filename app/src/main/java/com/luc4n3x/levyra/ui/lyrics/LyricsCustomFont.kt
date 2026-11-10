package com.luc4n3x.levyra.ui.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.luc4n3x.levyra.data.LyricsFontEntry
import com.luc4n3x.levyra.data.LyricsFontLibrary
import java.io.File
import java.util.concurrent.ConcurrentHashMap

val LocalLyricsCustomFontFamily = compositionLocalOf<FontFamily?> { null }

private val lyricsFontFamilies = ConcurrentHashMap<String, FontFamily>()

internal fun lyricsFontFamily(entry: LyricsFontEntry, file: File): FontFamily =
    lyricsFontFamilies.getOrPut(entry.fileName) {
        val range = entry.variableWeight
        if (range == null) {
            FontFamily(Font(file, FontWeight.Normal, variationSettings = FontVariation.Settings()))
        } else {
            FontFamily((100..900 step 100).map { weight ->
                Font(
                    file,
                    FontWeight(weight),
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight.coerceIn(range.first, range.last)))
                )
            })
        }
    }

@Composable
fun rememberLyricsFontFamily(fontId: String): FontFamily? {
    val context = LocalContext.current
    val library = remember(context) { LyricsFontLibrary.get(context) }
    val fonts by library.fonts.collectAsState()
    val entry = fonts.firstOrNull { it.id == fontId }
    val family by produceState<FontFamily?>(null, fontId, entry) {
        value = if (fontId.isBlank()) null else library.fontFile(fontId)?.let { (resolved, file) -> lyricsFontFamily(resolved, file) }
    }
    return family.takeIf { fontId.isNotBlank() }
}
