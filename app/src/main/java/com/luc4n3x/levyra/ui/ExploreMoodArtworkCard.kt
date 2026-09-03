package com.luc4n3x.levyra.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.SpotifyArtistArtworkRepository
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val ExploreMoodPortraitLookupSemaphore = Semaphore(2)
private const val ExploreRapFallbackPortraitUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/54/Eminem_in_2021.jpg/500px-Eminem_in_2021.jpg"

private val ExploreMoodGlobalArtistPools = mapOf(
    "nuove-uscite" to listOf("Billie Eilish", "The Weeknd", "Sabrina Carpenter", "Bruno Mars"),
    "local-wave" to listOf("Dua Lipa", "The Weeknd", "Billie Eilish", "Bad Bunny"),
    "rap-drill" to listOf("Eminem", "50 Cent", "Central Cee", "Travis Scott", "21 Savage", "Don Toliver"),
    "elettronica" to listOf("Fred again..", "Peggy Gou", "Calvin Harris", "David Guetta"),
    "pop-global" to listOf("Dua Lipa", "Billie Eilish", "Sabrina Carpenter", "Ariana Grande"),
    "rnb-soul" to listOf("SZA", "The Weeknd", "Brent Faiyaz", "Tems"),
    "rock-alt" to listOf("Måneskin", "Arctic Monkeys", "Paramore", "Linkin Park"),
    "latino" to listOf("Bad Bunny", "KAROL G", "Rauw Alejandro", "Feid"),
    "lofi-chill" to listOf("Joji", "Laufey", "beabadoobee", "keshi"),
    "anime-jpop" to listOf("Ado", "YOASOBI", "LiSA", "Kenshi Yonezu")
)

private val ExploreMoodLocalWaveArtistPools = mapOf(
    "it" to listOf("Annalisa", "Mahmood", "Elodie", "Lazza", "Geolier"),
    "es" to listOf("Rosalía", "Quevedo", "Aitana", "Rels B"),
    "fr" to listOf("Aya Nakamura", "GIMS", "Tiakola", "Angèle"),
    "de" to listOf("Apache 207", "Nina Chuba", "AYLIVA", "Luciano"),
    "pt" to listOf("Anitta", "Pedro Sampaio", "Luísa Sonza", "WIU"),
    "ro" to listOf("INNA", "The Motans", "Irina Rimes", "Delia"),
    "tr" to listOf("Ezhel", "Mabel Matiz", "Simge", "UZI"),
    "ru" to listOf("JONY", "Zivert", "MACAN", "Miyagi & Andy Panda"),
    "ar" to listOf("Wegz", "Marwan Pablo", "ElGrandeToto", "Saint Levant"),
    "zh" to listOf("Jay Chou", "G.E.M.", "Lexie Liu", "Joker Xue"),
    "ja" to listOf("YOASOBI", "Ado", "Fujii Kaze", "Kenshi Yonezu"),
    "ko" to listOf("aespa", "IVE", "Stray Kids", "NewJeans"),
    "hi" to listOf("Arijit Singh", "Diljit Dosanjh", "Badshah", "AP Dhillon"),
    "id" to listOf("NIKI", "Rich Brian", "Pamungkas", "Mahalini"),
    "vi" to listOf("Sơn Tùng M-TP", "tlinh", "HIEUTHUHAI", "Mỹ Anh"),
    "th" to listOf("MILLI", "Jeff Satur", "Tilly Birds", "PP Krit"),
    "fil" to listOf("BINI", "SB19", "Zack Tabudlo", "Lola Amour"),
    "he" to listOf("Noa Kirel", "Omer Adam", "Eden Hason", "Tuna")
)

private val ExploreMoodItalianRapArtists = listOf(
    "Sfera Ebbasta",
    "Shiva",
    "Geolier",
    "Tony Boy",
    "Kid Yugi"
)

internal fun exploreMoodPortraitCandidates(
    zoneId: String,
    languageCode: String,
    rotationBucket: Long
): List<String> {
    val language = languageCode.trim().lowercase().substringBefore('-').substringBefore('_')
    val primary = when {
        zoneId == "local-wave" -> ExploreMoodLocalWaveArtistPools[language]
            ?: ExploreMoodGlobalArtistPools[zoneId]
        zoneId == "rap-drill" && language == "it" -> ExploreMoodItalianRapArtists
        else -> ExploreMoodGlobalArtistPools[zoneId]
    }.orEmpty()
    if (primary.isEmpty()) return emptyList()

    val rotatedPrimary = rotateExploreMoodCandidates(
        candidates = primary,
        zoneId = zoneId,
        language = language,
        rotationBucket = rotationBucket
    )
    if (zoneId != "rap-drill" || language != "it") return rotatedPrimary

    val globalFallback = ExploreMoodGlobalArtistPools[zoneId].orEmpty()
    return buildList {
        addAll(rotatedPrimary.take(3))
        addAll(globalFallback.take(2))
        addAll(rotatedPrimary.drop(3))
        addAll(globalFallback.drop(2))
    }.distinct()
}

private fun rotateExploreMoodCandidates(
    candidates: List<String>,
    zoneId: String,
    language: String,
    rotationBucket: Long
): List<String> {
    if (candidates.size < 2) return candidates
    val base = Math.floorMod(31 * zoneId.hashCode() + language.hashCode(), candidates.size)
    val rotation = Math.floorMod(rotationBucket, candidates.size.toLong()).toInt()
    val offset = (base + rotation) % candidates.size
    return candidates.drop(offset) + candidates.take(offset)
}

internal fun exploreMoodPortraitArtist(
    zoneId: String,
    languageCode: String,
    rotationBucket: Long
): String? = exploreMoodPortraitCandidates(zoneId, languageCode, rotationBucket).firstOrNull()

internal fun exploreMoodPortraitLookupLimit(zoneId: String, candidateCount: Int): Int {
    val requested = if (zoneId == "rap-drill") 5 else 2
    return requested.coerceAtMost(candidateCount.coerceAtLeast(0))
}

internal fun exploreMoodHardFallbackPortraitUrl(zoneId: String): String =
    if (zoneId == "rap-drill") ExploreRapFallbackPortraitUrl else ""

@Composable
internal fun RowScope.ExploreMoodCard(
    zone: ExploreZone,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStartZoneMix: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val artworkRepository = remember(context) { SpotifyArtistArtworkRepository.get(context) }
    val rotationBucket = remember(zone.id) { exploreGenreRotationBucket(System.currentTimeMillis()) }
    val portraitCandidates = remember(zone.id, strings.code, rotationBucket) {
        exploreMoodPortraitCandidates(zone.id, strings.code, rotationBucket)
    }
    val portraitLookupLimit = remember(zone.id, portraitCandidates.size) {
        exploreMoodPortraitLookupLimit(zone.id, portraitCandidates.size)
    }
    val hardFallbackPortraitUrl = remember(zone.id) { exploreMoodHardFallbackPortraitUrl(zone.id) }
    var portraitCandidateIndex by remember(portraitCandidates) { mutableStateOf(0) }
    var portraitUrl by remember(portraitCandidates) { mutableStateOf("") }

    LaunchedEffect(portraitCandidates, portraitCandidateIndex, portraitLookupLimit, artworkRepository) {
        if (portraitCandidateIndex >= portraitLookupLimit) {
            portraitUrl = hardFallbackPortraitUrl
            return@LaunchedEffect
        }
        portraitUrl = ""
        val resolved = ExploreMoodPortraitLookupSemaphore.withPermit {
            artworkRepository.resolveArtistPortrait(portraitCandidates[portraitCandidateIndex])
        }
        if (resolved.isBlank()) {
            portraitCandidateIndex += 1
        } else {
            portraitUrl = resolved
        }
    }

    val accentStart = Color(zone.accentStart)
    val accentEnd = Color(zone.accentEnd)
    val shape = RoundedCornerShape(18.dp)
    val backgroundBrush = remember(accentStart, accentEnd) {
        Brush.linearGradient(
            listOf(
                LevyraPanel,
                accentStart.copy(alpha = 0.26f),
                accentEnd.copy(alpha = 0.18f)
            )
        )
    }
    val imageScrim = remember(accentStart) {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to LevyraPanel,
                0.34f to LevyraPanel.copy(alpha = 0.94f),
                0.58f to accentStart.copy(alpha = 0.38f),
                0.80f to Color.Transparent,
                1f to Color.Transparent
            )
        )
    }
    val bottomScrim = remember {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = 0.36f)
            )
        )
    }
    val outlineBrush = remember(accentStart, accentEnd, isSelected) {
        Brush.linearGradient(
            listOf(
                accentStart.copy(alpha = if (isSelected) 0.96f else 0.54f),
                accentEnd.copy(alpha = if (isSelected) 0.72f else 0.30f),
                Color.White.copy(alpha = if (isSelected) 0.18f else 0.08f)
            )
        )
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(108.dp)
            .clip(shape)
            .background(backgroundBrush)
            .border(
                BorderStroke(if (isSelected) 1.5.dp else 1.dp, outlineBrush),
                shape
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = isSelected
            }
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button,
                onClickLabel = zone.label,
                onLongClick = onStartZoneMix,
                onLongClickLabel = strings.mixStartRadio
            )
    ) {
        if (portraitUrl.isNotBlank()) {
            val activePortraitUrl = portraitUrl
            AsyncImage(
                model = activePortraitUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                onError = {
                    if (portraitUrl == activePortraitUrl) {
                        portraitUrl = ""
                        if (activePortraitUrl != hardFallbackPortraitUrl) {
                            portraitCandidateIndex = (portraitCandidateIndex + 1).coerceAtMost(portraitLookupLimit)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.72f)
            )
        } else {
            Text(
                text = zone.emoji,
                color = Color.White.copy(alpha = 0.28f),
                fontSize = 42.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(imageScrim))
        Box(modifier = Modifier.fillMaxSize().background(bottomScrim))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 14.dp, end = 8.dp, bottom = 13.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .width(if (isSelected) 30.dp else 22.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Color.White else accentEnd.copy(alpha = 0.92f))
            )
            Text(
                text = zone.label,
                color = Color.White,
                fontSize = 16.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(16.5.sp),
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 7.dp)
            )
        }
    }
}
