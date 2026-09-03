package com.luc4n3x.levyra.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

private val ExploreMoodGlobalArtistPools = mapOf(
    "nuove-uscite" to listOf("Billie Eilish", "The Weeknd", "Sabrina Carpenter", "Bruno Mars"),
    "local-wave" to listOf("Dua Lipa", "The Weeknd", "Billie Eilish", "Bad Bunny"),
    "rap-drill" to listOf("Central Cee", "Travis Scott", "21 Savage", "Don Toliver"),
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

internal fun exploreMoodPortraitArtist(
    zoneId: String,
    languageCode: String,
    rotationBucket: Long
): String? {
    val language = languageCode.trim().lowercase().substringBefore('-').substringBefore('_')
    val candidates = when {
        zoneId == "local-wave" -> ExploreMoodLocalWaveArtistPools[language]
            ?: ExploreMoodGlobalArtistPools[zoneId]
        zoneId == "rap-drill" && language == "it" -> ExploreMoodItalianRapArtists
        else -> ExploreMoodGlobalArtistPools[zoneId]
    }.orEmpty()
    if (candidates.isEmpty()) return null
    val base = Math.floorMod(31 * zoneId.hashCode() + language.hashCode(), candidates.size)
    val rotation = Math.floorMod(rotationBucket, candidates.size.toLong()).toInt()
    return candidates[(base + rotation) % candidates.size]
}

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
    val portraitArtist = remember(zone.id, strings.code, rotationBucket) {
        exploreMoodPortraitArtist(zone.id, strings.code, rotationBucket)
    }
    var portraitUrl by remember(portraitArtist) { mutableStateOf("") }

    LaunchedEffect(portraitArtist, artworkRepository) {
        portraitUrl = ""
        val artist = portraitArtist ?: return@LaunchedEffect
        portraitUrl = ExploreMoodPortraitLookupSemaphore.withPermit {
            artworkRepository.resolveArtistPortrait(artist)
        }
    }

    val accentStart = Color(zone.accentStart)
    val accentEnd = Color(zone.accentEnd)
    val shape = RoundedCornerShape(18.dp)
    val fallbackBrush = remember(accentStart, accentEnd) {
        Brush.linearGradient(
            listOf(
                accentStart.copy(alpha = 0.62f),
                accentEnd.copy(alpha = 0.42f),
                LevyraPanel
            )
        )
    }
    val artworkScrim = remember(accentStart, accentEnd) {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to accentStart.copy(alpha = 0.96f),
                0.42f to accentStart.copy(alpha = 0.78f),
                0.70f to accentEnd.copy(alpha = 0.30f),
                1f to Color.Black.copy(alpha = 0.12f)
            )
        )
    }
    val lowerScrim = remember {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.18f),
                Color.Black.copy(alpha = 0.58f)
            )
        )
    }
    val outlineBrush = remember(accentStart, accentEnd, isSelected) {
        Brush.linearGradient(
            listOf(
                accentStart.copy(alpha = if (isSelected) 0.98f else 0.72f),
                accentEnd.copy(alpha = if (isSelected) 0.82f else 0.42f),
                Color.White.copy(alpha = if (isSelected) 0.22f else 0.10f)
            )
        )
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .height(104.dp)
            .clip(shape)
            .background(fallbackBrush)
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
            AsyncImage(
                model = portraitUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterEnd,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = zone.emoji,
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 44.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(artworkScrim))
        Box(modifier = Modifier.fillMaxSize().background(lowerScrim))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 10.dp, bottom = 13.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentEnd.copy(alpha = 0.95f))
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

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.92f))
            )
        }
    }
}
