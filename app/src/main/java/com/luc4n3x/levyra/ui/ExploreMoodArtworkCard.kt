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
private const val ExploreRapFallbackPortraitUrl = "https://upload.wikimedia.org/wikipedia/commons/5/54/Eminem_in_2021.jpg"

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
    "anime-jpop" to listOf("Ado", "YOASOBI", "LiSA", "Kenshi Yonezu"),
    "mood-workout" to listOf("Travis Scott", "Future", "David Guetta", "Calvin Harris", "21 Savage"),
    "mood-focus" to listOf("Ludovico Einaudi", "Hans Zimmer", "Max Richter", "Bonobo"),
    "mood-party" to listOf("Dua Lipa", "Calvin Harris", "Rihanna", "Bad Bunny", "Charli xcx"),
    "mood-drive" to listOf("The Weeknd", "Post Malone", "Don Toliver", "Tame Impala"),
    "mood-sad" to listOf("Billie Eilish", "Olivia Rodrigo", "Lana Del Rey", "Mitski"),
    "mood-sleep" to listOf("Max Richter", "Brian Eno", "Sigur Rós", "Ludovico Einaudi"),
    "mood-feel-good" to listOf("Bruno Mars", "Sabrina Carpenter", "Dua Lipa", "Pharrell Williams"),
    "mood-romance" to listOf("SZA", "Frank Ocean", "Daniel Caesar", "Lana Del Rey"),
    "genre-indie" to listOf("Tame Impala", "Arctic Monkeys", "The Strokes", "Phoebe Bridgers"),
    "genre-metal" to listOf("Metallica", "Slipknot", "Bring Me The Horizon", "Linkin Park"),
    "genre-kpop" to listOf("aespa", "NewJeans", "Stray Kids", "IVE"),
    "genre-jazz" to listOf("Laufey", "Norah Jones", "Kamasi Washington", "Robert Glasper"),
    "genre-classical" to listOf("Ludovico Einaudi", "Yo-Yo Ma", "Lang Lang", "Max Richter"),
    "genre-country" to listOf("Zach Bryan", "Morgan Wallen", "Kacey Musgraves", "Chris Stapleton"),
    "genre-reggae" to listOf("Bob Marley & The Wailers", "Sean Paul", "Koffee", "Burna Boy"),
    "genre-afro" to listOf("Burna Boy", "Tems", "Rema", "Ayra Starr"),
    "genre-folk" to listOf("Hozier", "Bon Iver", "Phoebe Bridgers", "Noah Kahan"),
    "genre-blues" to listOf("Gary Clark Jr.", "John Mayer", "The Black Keys", "B.B. King")
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
    onStartZoneMix: (() -> Unit)? = null
) {
    MoodGenreCard(
        id = zone.id,
        title = zone.label,
        portraitKey = zone.id,
        accentStart = zone.accentStart,
        accentEnd = zone.accentEnd,
        emoji = zone.emoji,
        externalArtworkUrl = "",
        isSelected = isSelected,
        compact = true,
        modifier = Modifier.weight(1f),
        onClick = onClick,
        onStartMix = onStartZoneMix
    )
}

@Composable
internal fun RowScope.MoodGenreCard(
    item: ExploreUnifiedItem,
    artworkUrl: String?,
    isSelected: Boolean,
    compact: Boolean = true,
    onClick: () -> Unit,
    onStartZoneMix: (() -> Unit)? = null
) {
    MoodGenreCard(
        id = item.id,
        title = item.title,
        portraitKey = item.portraitKey,
        accentStart = item.accentStart,
        accentEnd = item.accentEnd,
        emoji = item.emoji,
        externalArtworkUrl = artworkUrl.orEmpty(),
        isSelected = isSelected,
        compact = compact,
        modifier = Modifier.weight(1f),
        onClick = onClick,
        onStartMix = onStartZoneMix
    )
}

private data class MoodGenrePortraitState(
    val url: String,
    val onArtworkError: (String) -> Unit
)

@Composable
private fun rememberMoodGenrePortraitState(
    id: String,
    portraitKey: String,
    languageCode: String
): MoodGenrePortraitState {
    val context = LocalContext.current
    val artworkRepository = remember(context) { SpotifyArtistArtworkRepository.get(context) }
    val rotationBucket = remember(id) { exploreGenreRotationBucket(System.currentTimeMillis()) }
    val portraitCandidates = remember(portraitKey, languageCode, rotationBucket) {
        exploreMoodPortraitCandidates(portraitKey, languageCode, rotationBucket)
    }
    val portraitLookupLimit = remember(portraitKey, portraitCandidates.size) {
        exploreMoodPortraitLookupLimit(portraitKey, portraitCandidates.size)
    }
    val hardFallbackPortraitUrl = remember(portraitKey) { exploreMoodHardFallbackPortraitUrl(portraitKey) }
    var portraitCandidateIndex by remember(portraitCandidates) { mutableStateOf(0) }
    var portraitUrl by remember(portraitCandidates, hardFallbackPortraitUrl) {
        mutableStateOf(hardFallbackPortraitUrl)
    }

    LaunchedEffect(portraitCandidates, portraitCandidateIndex, portraitLookupLimit, artworkRepository) {
        if (portraitCandidateIndex >= portraitLookupLimit) {
            portraitUrl = hardFallbackPortraitUrl
            return@LaunchedEffect
        }
        if (hardFallbackPortraitUrl.isBlank()) portraitUrl = ""
        val resolved = ExploreMoodPortraitLookupSemaphore.withPermit {
            artworkRepository.resolveArtistPortrait(portraitCandidates[portraitCandidateIndex])
        }
        if (resolved.isBlank()) {
            portraitCandidateIndex += 1
        } else {
            portraitUrl = resolved
        }
    }

    return MoodGenrePortraitState(
        url = portraitUrl,
        onArtworkError = { failedUrl ->
            if (portraitUrl == failedUrl) {
                if (failedUrl == hardFallbackPortraitUrl) {
                    portraitUrl = ""
                } else {
                    portraitUrl = hardFallbackPortraitUrl
                    portraitCandidateIndex = (portraitCandidateIndex + 1).coerceAtMost(portraitLookupLimit)
                }
            }
        }
    )
}

@Composable
internal fun MoodGenreCard(
    id: String,
    title: String,
    portraitKey: String,
    accentStart: Int,
    accentEnd: Int,
    emoji: String = "",
    externalArtworkUrl: String = "",
    isSelected: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onStartMix: (() -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val portraitState = rememberMoodGenrePortraitState(id = id, portraitKey = portraitKey, languageCode = strings.code)
    val effectiveArtworkUrl = portraitState.url.ifBlank { externalArtworkUrl }
    val startColor = Color(accentStart)
    val endColor = Color(accentEnd)
    val cardHeight = if (compact) 104.dp else 110.dp
    val shape = RoundedCornerShape(16.dp)
    val backgroundBrush = remember(startColor, endColor) {
        Brush.linearGradient(
            listOf(
                LevyraPanel,
                startColor.copy(alpha = 0.25f),
                endColor.copy(alpha = 0.17f)
            )
        )
    }
    val outlineBrush = rememberMoodGenreOutlineBrush(startColor, endColor, isSelected)

    Box(
        modifier = modifier
            .height(cardHeight)
            .clip(shape)
            .background(backgroundBrush)
            .border(BorderStroke(if (isSelected) 1.5.dp else 1.dp, outlineBrush), shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = isSelected
            }
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button,
                onClickLabel = title,
                onLongClick = onStartMix,
                onLongClickLabel = onStartMix?.let { strings.mixStartRadio }
            )
    ) {
        MoodGenreCardBody(
            title = title,
            emoji = emoji,
            artworkUrl = effectiveArtworkUrl,
            startColor = startColor,
            endColor = endColor,
            isSelected = isSelected,
            onArtworkError = portraitState.onArtworkError
        )
    }
}

@Composable
private fun rememberMoodGenreOutlineBrush(
    startColor: Color,
    endColor: Color,
    isSelected: Boolean
): Brush = remember(startColor, endColor, isSelected) {
    val startAlpha = if (isSelected) 0.94f else 0.46f
    val endAlpha = if (isSelected) 0.70f else 0.26f
    val whiteAlpha = if (isSelected) 0.18f else 0.08f
    Brush.linearGradient(
        listOf(
            startColor.copy(alpha = startAlpha),
            endColor.copy(alpha = endAlpha),
            Color.White.copy(alpha = whiteAlpha)
        )
    )
}

@Composable
private fun MoodGenreCardBody(
    title: String,
    emoji: String,
    artworkUrl: String,
    startColor: Color,
    endColor: Color,
    isSelected: Boolean,
    onArtworkError: (String) -> Unit
) {
    val imageScrim = remember(startColor) {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to LevyraPanel,
                0.35f to LevyraPanel.copy(alpha = 0.94f),
                0.58f to startColor.copy(alpha = 0.36f),
                0.82f to Color.Transparent,
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
    val titleSize = if (title.length >= 18) 15.sp else 16.sp

    Box(modifier = Modifier.fillMaxSize()) {
        if (artworkUrl.isNotBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                onError = { onArtworkError(artworkUrl) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.70f)
            )
        } else if (emoji.isNotBlank()) {
            Text(
                text = emoji,
                color = Color.White.copy(alpha = 0.22f),
                fontSize = 38.sp,
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
                .fillMaxWidth(0.76f)
                .padding(start = 14.dp, end = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .width(if (isSelected) 28.dp else 20.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Color.White else endColor.copy(alpha = 0.92f))
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = titleSize,
                lineHeight = LevyraTypeRhythm.lineHeight(titleSize),
                letterSpacing = (-0.2).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}