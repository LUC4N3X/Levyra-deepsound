package com.luc4n3x.levyra.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.SpotifyArtistArtworkRepository
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale

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

internal fun exploreMoodPortraitWindow(
    zoneId: String,
    zoneIds: List<String>,
    languageCode: String,
    rotationBucket: Long
): List<String> {
    val claimed = HashSet<String>()
    for (candidateZoneId in (zoneIds + zoneId).distinct()) {
        val available = exploreMoodPortraitCandidates(candidateZoneId, languageCode, rotationBucket)
            .filterNot { artist -> exploreMoodArtistKey(artist) in claimed }
        val window = available.take(exploreMoodPortraitLookupLimit(candidateZoneId, available.size))
        if (candidateZoneId == zoneId) return window
        window.forEach { artist -> claimed += exploreMoodArtistKey(artist) }
    }
    return emptyList()
}

private fun exploreMoodArtistKey(artist: String): String = artist.trim().lowercase(Locale.ROOT)

internal fun exploreMoodHardFallbackPortraitUrl(zoneId: String): String =
    if (zoneId == "rap-drill") ExploreRapFallbackPortraitUrl else ""


@Composable
internal fun rememberExploreMoodArtworkUrl(zone: ExploreZone): String {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val artworkRepository = remember(context) { SpotifyArtistArtworkRepository.get(context) }
    val rotationBucket = remember(zone.id) { exploreGenreRotationBucket(System.currentTimeMillis()) }
    val portraitCandidates = remember(zone.id, strings, rotationBucket) {
        exploreMoodPortraitWindow(
            zoneId = zone.id,
            zoneIds = ExploreCatalog.getZones(strings).map { it.id },
            languageCode = strings.code,
            rotationBucket = rotationBucket
        )
    }
    val portraitLookupLimit = portraitCandidates.size
    val hardFallbackPortraitUrl = remember(zone.id) { exploreMoodHardFallbackPortraitUrl(zone.id) }
    var artworkUrl by remember(portraitCandidates, hardFallbackPortraitUrl) {
        mutableStateOf(hardFallbackPortraitUrl)
    }

    LaunchedEffect(portraitCandidates, portraitLookupLimit, artworkRepository, hardFallbackPortraitUrl) {
        val resolved = ExploreMoodPortraitLookupSemaphore.withPermit {
            var candidateArtwork = ""
            for (candidate in portraitCandidates.take(portraitLookupLimit)) {
                candidateArtwork = artworkRepository.resolveArtistPortrait(candidate)
                if (candidateArtwork.isNotBlank()) break
            }
            candidateArtwork
        }
        artworkUrl = resolved.ifBlank { hardFallbackPortraitUrl }
    }

    return artworkUrl
}

@Composable
internal fun RowScope.ExploreMoodCard(
    zone: ExploreZone,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStartZoneMix: (() -> Unit)? = null,
    prominent: Boolean = false
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val artworkRepository = remember(context) { SpotifyArtistArtworkRepository.get(context) }
    val rotationBucket = remember(zone.id) { exploreGenreRotationBucket(System.currentTimeMillis()) }
    val portraitCandidates = remember(zone.id, strings, rotationBucket) {
        exploreMoodPortraitWindow(
            zoneId = zone.id,
            zoneIds = ExploreCatalog.getZones(strings).map { it.id },
            languageCode = strings.code,
            rotationBucket = rotationBucket
        )
    }
    val portraitLookupLimit = portraitCandidates.size
    val hardFallbackPortraitUrl = remember(zone.id) { exploreMoodHardFallbackPortraitUrl(zone.id) }
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

    val accentStart = Color(zone.accentStart)
    val accentEnd = Color(zone.accentEnd)
    val shape = LevyraCardDesign.EditorialShape
    val ambientScrim = remember(accentStart, accentEnd) {
        Brush.horizontalGradient(
            listOf(accentStart.copy(alpha = 0.11f), accentEnd.copy(alpha = 0.035f), Color.Transparent)
        )
    }
    val bottomScrim = remember {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.30f),
                Color.Black.copy(alpha = 0.92f)
            )
        )
    }
    val colors = MaterialTheme.colorScheme
    val outlineBrush = remember(colors, isSelected) {
        Brush.linearGradient(
            listOf(
                if (isSelected) colors.primary.copy(alpha = 0.78f) else colors.outlineVariant.copy(alpha = 0.58f),
                if (isSelected) colors.primary.copy(alpha = 0.45f) else colors.outlineVariant.copy(alpha = 0.28f)
            )
        )
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = if (prominent) 188.dp else 144.dp)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
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
                onLongClickLabel = if (onStartZoneMix != null) strings.mixStartRadio else null
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
                        if (activePortraitUrl == hardFallbackPortraitUrl) {
                            portraitUrl = ""
                        } else {
                            portraitUrl = hardFallbackPortraitUrl
                            portraitCandidateIndex = (portraitCandidateIndex + 1).coerceAtMost(portraitLookupLimit)
                        }
                    }
                },
                modifier = Modifier
                    .matchParentSize()
            )
        }

        Box(modifier = Modifier.matchParentSize().background(ambientScrim))
        Box(modifier = Modifier.matchParentSize().background(bottomScrim))

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = zone.label,
                color = Color.White,
                style = LevyraType.sectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    brush = outlineBrush,
                    shape = shape
                )
        )
    }
}
