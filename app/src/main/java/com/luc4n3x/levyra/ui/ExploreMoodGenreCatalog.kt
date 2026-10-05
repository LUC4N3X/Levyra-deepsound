package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import java.text.Normalizer
import java.util.Locale

internal const val ExploreFeaturedMoodLimit = 6
internal const val ExploreFeaturedGenreLimit = 6

internal enum class ExploreUnifiedKind {
    Mood,
    Genre
}

@Immutable
internal data class ExploreUnifiedItem(
    val id: String,
    val canonicalKey: String,
    val kind: ExploreUnifiedKind,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val query: String,
    val accentStart: Int,
    val accentEnd: Int,
    val portraitKey: String,
    val zone: ExploreZone,
    val providerCategory: ExploreCategory? = null
) {
    val label: String
        get() = title
}

@Immutable
internal data class ExploreUnifiedCatalog(
    val fullMoods: List<ExploreUnifiedItem>,
    val featuredMoods: List<ExploreUnifiedItem>,
    val fullGenres: List<ExploreUnifiedItem>,
    val featuredGenres: List<ExploreUnifiedItem>,
    val allZones: List<ExploreZone>
)

private data class CanonicalCategorySpec(
    val canonicalKey: String,
    val kind: ExploreUnifiedKind,
    val portraitKey: String,
    val accentStart: Int,
    val accentEnd: Int,
    val emoji: String,
    val aliases: Set<String>
)

private val CanonicalMoodSpecs = listOf(
    CanonicalCategorySpec(
        canonicalKey = "lofi-chill",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "lofi-chill",
        accentStart = 0xFF11998E.toInt(),
        accentEnd = 0xFF38EF7D.toInt(),
        emoji = "😌",
        aliases = setOf(
            "chill", "relax", "relaxing", "lofi", "lo fi", "lofi chill", "lo fi chill",
            "calm", "peaceful", "ambient", "rilassamento", "rilassante", "calma",
            "entspannung", "detente", "relajacion", "チル", "リラックス", "칠", "휴식", "放松", "استرخاء"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-workout",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-workout",
        accentStart = 0xFF1B5CFF.toInt(),
        accentEnd = 0xFF00E5FF.toInt(),
        emoji = "🏋️",
        aliases = setOf(
            "workout", "gym", "fitness", "allenamento", "palestra", "sport", "training",
            "cardio", "running", "academia", "gimnasio", "entrainement", "kraft",
            "ワークアウト", "운동", "피트니스", "健身", "تمرين", "رياضة"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-focus",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-focus",
        accentStart = 0xFF6A11CB.toInt(),
        accentEnd = 0xFF2575FC.toInt(),
        emoji = "🎧",
        aliases = setOf(
            "focus", "study", "concentration", "concentrazione", "studio", "coding",
            "reading", "work", "lavoro", "foco", "etude", "konzentration", "lernen",
            "集中", "勉強", "집중", "공부", "专注", "学习", "تركيز"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-party",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-party",
        accentStart = 0xFFFF7A18.toInt(),
        accentEnd = 0xFFFF3B5C.toInt(),
        emoji = "🎉",
        aliases = setOf(
            "party", "festa", "fiesta", "fete", "feier", "clubbing", "nightlife",
            "パーティー", "파티", "派对", "حفلة"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-drive",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-drive",
        accentStart = 0xFF8E2DE2.toInt(),
        accentEnd = 0xFF4A00E0.toInt(),
        emoji = "🚗",
        aliases = setOf(
            "drive", "driving", "commute", "in auto", "auto", "car", "road trip",
            "night drive", "viaggio", "conduciendo", "en voiture", "no carro", "unterwegs",
            "ドライブ", "通勤", "드라이브", "출퇴근", "驾车", "通勤", "قيادة"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-sad",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-sad",
        accentStart = 0xFF355C7D.toInt(),
        accentEnd = 0xFFC06C84.toInt(),
        emoji = "💔",
        aliases = setOf(
            "sad", "malinconia", "melancholy", "melancolia", "heartbreak", "cry",
            "triste", "tristezza", "melancholie", "traurig",
            "切なさ", "悲しい", "슬픔", "우울", "伤感", "مشاعر", "حزين"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-sleep",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-sleep",
        accentStart = 0xFF1E3C72.toInt(),
        accentEnd = 0xFF2A5298.toInt(),
        emoji = "🌙",
        aliases = setOf(
            "sleep", "sleeping", "sonno", "dormire", "dormir", "sommeil", "schlaf", "schlafen",
            "bedtime", "night", "notte", "睡眠", "수면", "نوم"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-feel-good",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-feel-good",
        accentStart = 0xFFFF9966.toInt(),
        accentEnd = 0xFFFF5E62.toInt(),
        emoji = "☀️",
        aliases = setOf(
            "feel good", "feelgood", "happy", "buonumore", "felice", "good vibes",
            "positive", "allegria", "felicidad", "bonne humeur", "gute laune",
            "ハッピー", "기분전환", "행복", "快乐", "سعادة"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-romance",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-romance",
        accentStart = 0xFFEC008C.toInt(),
        accentEnd = 0xFFFC6767.toInt(),
        emoji = "💜",
        aliases = setOf(
            "romance", "romantic", "amore", "love", "romantico", "romantique",
            "romantik", "liebe", "ロマンス", "恋愛", "로맨스", "사랑", "浪漫", "رومانسية"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "mood-energy",
        kind = ExploreUnifiedKind.Mood,
        portraitKey = "mood-workout",
        accentStart = 0xFFFF512F.toInt(),
        accentEnd = 0xFFDD2476.toInt(),
        emoji = "⚡",
        aliases = setOf(
            "energy", "energy boosters", "energia", "carica", "energico", "energie",
            "energi", "パワー", "에너지", "能量", "طاقة"
        )
    )
)

private val CanonicalGenreSpecs = listOf(
    CanonicalCategorySpec(
        canonicalKey = "pop-global",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "pop-global",
        accentStart = 0xFFFF4081.toInt(),
        accentEnd = 0xFF7C4DFF.toInt(),
        emoji = "🌍",
        aliases = setOf(
            "pop", "pop global", "global pop", "top pop", "pop hits", "pop music",
            "ポップ", "ポップス", "팝", "流行"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "rap-drill",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "rap-drill",
        accentStart = 0xFF9D4EDD.toInt(),
        accentEnd = 0xFF7C4DFF.toInt(),
        emoji = "🎤",
        aliases = setOf(
            "rap", "hip hop", "hiphop", "drill", "trap", "rap drill", "hip hop rap",
            "rap hip hop", "urban", "ヒップホップ", "ラップ", "힙합", "랩", "说唱", "嘻哈", "هيب هوب", "راب"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "elettronica",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "elettronica",
        accentStart = 0xFF18FFFF.toInt(),
        accentEnd = 0xFF9D4EDD.toInt(),
        emoji = "⚡",
        aliases = setOf(
            "electronic", "elettronica", "dance", "edm", "house", "techno",
            "dance electronic", "electronic dance", "electronica", "electronique",
            "elektronisch", "eletronica", "エレクトロニック", "ダンス", "일렉트로닉", "댄스", "电子", "إلكترونيك"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "rock-alt",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "rock-alt",
        accentStart = 0xFFFF6E40.toInt(),
        accentEnd = 0xFFFF1744.toInt(),
        emoji = "🎸",
        aliases = setOf(
            "rock", "alternative", "alt rock", "rock alt", "rock alternative",
            "classic rock", "hard rock", "ロック", "オルタナティブ", "록", "얼터너티브", "摇滚", "روك"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "rnb-soul",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "rnb-soul",
        accentStart = 0xFFB388FF.toInt(),
        accentEnd = 0xFFFF4081.toInt(),
        emoji = "🌒",
        aliases = setOf(
            "r b", "rnb", "soul", "r b soul", "rnb soul", "neo soul", "funk soul", "funk",
            "アールアンドビー", "ソウル", "알앤비", "소울", "节奏布鲁斯", "آر أند بي"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "latino",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "latino",
        accentStart = 0xFFFFC400.toInt(),
        accentEnd = 0xFFFF6E40.toInt(),
        emoji = "🔥",
        aliases = setOf(
            "latin", "latino", "musica latina", "reggaeton", "urbano latino", "latina",
            "ラテン", "라틴", "拉丁", "لاتيني"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "local-wave",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "local-wave",
        accentStart = 0xFF00E676.toInt(),
        accentEnd = 0xFF00B0FF.toInt(),
        emoji = "🌊",
        aliases = setOf(
            "local wave", "italia", "musica italiana", "italiana", "italian",
            "espana", "france", "chanson", "deutsch", "brasil", "mpb", "sertanejo",
            "邦楽", "가요", "국내", "华语", "عربي"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "anime-jpop",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "anime-jpop",
        accentStart = 0xFFFF5252.toInt(),
        accentEnd = 0xFFB388FF.toInt(),
        emoji = "🏮",
        aliases = setOf(
            "anime", "j pop", "jpop", "anime j pop", "anime jpop", "japanese pop",
            "アニメ", "제이팝", "애니메이션", "动漫"
        )
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-indie",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-indie",
        accentStart = 0xFF00B4DB.toInt(),
        accentEnd = 0xFF0083B0.toInt(),
        emoji = "✨",
        aliases = setOf("indie", "indie rock", "indie pop", "インディー", "인디", "独立")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-metal",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-metal",
        accentStart = 0xFF870000.toInt(),
        accentEnd = 0xFF190A05.toInt(),
        emoji = "🤘",
        aliases = setOf("metal", "heavy metal", "メタル", "ヘヴィメタル", "메탈", "金属")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-kpop",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-kpop",
        accentStart = 0xFFFF0099.toInt(),
        accentEnd = 0xFF493240.toInt(),
        emoji = "💫",
        aliases = setOf("k pop", "kpop", "korean pop", "케이팝", "韩流")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-jazz",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-jazz",
        accentStart = 0xFFD38312.toInt(),
        accentEnd = 0xFFA83279.toInt(),
        emoji = "🎷",
        aliases = setOf("jazz", "ジャズ", "재즈", "爵士", "جاز")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-classical",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-classical",
        accentStart = 0xFF536976.toInt(),
        accentEnd = 0xFF292E49.toInt(),
        emoji = "🎻",
        aliases = setOf("classical", "classica", "clasica", "classique", "klassik", "クラシック", "클래식", "古典", "كلاسيك")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-country",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-country",
        accentStart = 0xFFE65C00.toInt(),
        accentEnd = 0xFFF9D423.toInt(),
        emoji = "🤠",
        aliases = setOf("country", "americana", "カントリー", "컨트리", "乡村")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-reggae",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-reggae",
        accentStart = 0xFF11998E.toInt(),
        accentEnd = 0xFFF7971E.toInt(),
        emoji = "🌴",
        aliases = setOf("reggae", "dancehall", "caribbean", "レゲエ", "레게", "雷鬼")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-afro",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-afro",
        accentStart = 0xFFF12711.toInt(),
        accentEnd = 0xFFF5AF19.toInt(),
        emoji = "🥁",
        aliases = setOf("afro", "afrobeats", "afrobeat", "african", "アフロビーツ", "아프로비츠")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-folk",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-folk",
        accentStart = 0xFF5A3F37.toInt(),
        accentEnd = 0xFF2C7744.toInt(),
        emoji = "🪕",
        aliases = setOf("folk", "acoustic", "acustica", "folk acoustic", "フォーク", "アコースティック", "포크", "어쿠스틱", "民谣")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-blues",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "genre-blues",
        accentStart = 0xFF1A2980.toInt(),
        accentEnd = 0xFF26D0CE.toInt(),
        emoji = "🎺",
        aliases = setOf("blues", "ブルース", "블루스", "蓝调")
    ),
    CanonicalCategorySpec(
        canonicalKey = "genre-soundtrack",
        kind = ExploreUnifiedKind.Genre,
        portraitKey = "mood-focus",
        accentStart = 0xFF3A1C71.toInt(),
        accentEnd = 0xFFD76D77.toInt(),
        emoji = "🎬",
        aliases = setOf("soundtrack", "soundtracks", "colonne sonore", "film", "ost", "サウンドトラック", "사운드트랙", "原声")
    )
)

private val NonAlphaNumericRegex = Regex("[^\\p{L}\\p{N}]+")
private val DiacriticsRegex = Regex("\\p{Mn}+")
private val MultiSpaceRegex = Regex("\\s+")

internal fun normalizeExploreCategoryToken(raw: String): String {
    if (raw.isBlank()) return ""
    val decomposed = Normalizer.normalize(raw.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
    val withoutMarks = decomposed.replace(DiacriticsRegex, "")
    return withoutMarks
        .replace("&", " ")
        .replace(NonAlphaNumericRegex, " ")
        .replace(MultiSpaceRegex, " ")
        .trim()
}

internal fun canonicalExploreCategoryKey(
    title: String,
    sectionIndex: Int = -1
): String? = matchCanonicalSpec(
    ExploreCategory(
        title = title,
        params = "lookup",
        section = "",
        sectionIndex = sectionIndex
    )
)?.canonicalKey

private fun matchCanonicalSpec(
    category: ExploreCategory
): CanonicalCategorySpec? {
    val normalizedTitle = normalizeExploreCategoryToken(category.title)
    if (normalizedTitle.isBlank()) return null

    val allSpecs = if (category.sectionIndex == 0) {
        CanonicalMoodSpecs + CanonicalGenreSpecs
    } else {
        CanonicalGenreSpecs + CanonicalMoodSpecs
    }

    allSpecs.firstOrNull { spec ->
        normalizedTitle in spec.aliases
    }?.let { return it }

    val titleWords = normalizedTitle.split(' ').filter { it.length >= 2 }.toSet()
    if (titleWords.isEmpty()) return null

    return allSpecs.firstOrNull { spec ->
        spec.aliases.any { alias ->
            val aliasWords = alias.split(' ').filter { it.isNotBlank() }
            if (aliasWords.size == 1) {
                aliasWords.single() in titleWords
            } else {
                aliasWords.all { word -> word in titleWords } || normalizedTitle.contains(alias)
            }
        }
    }
}

internal fun buildUnifiedExploreCatalog(
    strings: LevyraStrings,
    zones: List<ExploreZone> = ExploreCatalog.getZones(strings),
    categories: List<ExploreCategory> = emptyList()
): ExploreUnifiedCatalog {
    val moodEngine = MoodEngine()
    val nativeMoodsById = moodEngine.moodsForLanguage(strings.code).associateBy { it.id }
    val zonesById = zones.associateBy { it.id }
    val contentLocale = LevyraContentLocales.forLanguage(strings.code)

    val matchedProviderByCanonical = LinkedHashMap<String, ExploreCategory>()
    val unmatchedMoodCategories = ArrayList<ExploreCategory>()
    val unmatchedGenreCategories = ArrayList<ExploreCategory>()
    val seenUnmatchedKeys = HashSet<String>()

    categories.forEach { rawCategory ->
        val title = rawCategory.title.trim()
        val params = rawCategory.params.trim()
        if (title.isBlank() || params.isBlank()) return@forEach
        val cleanCategory = rawCategory.copy(title = title, section = rawCategory.section.trim(), params = params)

        val matchedSpec = matchCanonicalSpec(cleanCategory)
        if (matchedSpec != null) {
            matchedProviderByCanonical.putIfAbsent(matchedSpec.canonicalKey, cleanCategory)
        } else {
            val normalizedKey = normalizeExploreCategoryToken(title).ifBlank { params }
            if (seenUnmatchedKeys.add(normalizedKey)) {
                if (cleanCategory.sectionIndex == 0) {
                    unmatchedMoodCategories.add(cleanCategory)
                } else {
                    unmatchedGenreCategories.add(cleanCategory)
                }
            }
        }
    }

    fun buildMoodItem(
        canonicalKey: String,
        moodId: String,
        fallbackZoneId: String? = null
    ): ExploreUnifiedItem? {
        val spec = CanonicalMoodSpecs.firstOrNull { it.canonicalKey == canonicalKey } ?: return null
        val mood = nativeMoodsById[moodId]
        val baseZone = fallbackZoneId?.let { zonesById[it] }
        val provider = matchedProviderByCanonical[canonicalKey]
        val title = mood?.title ?: baseZone?.label ?: provider?.title ?: return null
        val subtitle = mood?.subtitle ?: strings.exploreMoodSection
        val query = if (mood != null) {
            moodEngine.tagQueryFor(mood, strings.code)
        } else {
            baseZone?.query ?: "$title music"
        }
        val zone = baseZone?.copy(label = title) ?: ExploreZone(
            id = canonicalKey,
            label = title,
            emoji = mood?.icon ?: spec.emoji,
            query = query,
            accentStart = mood?.accentStart ?: spec.accentStart,
            accentEnd = mood?.accentEnd ?: spec.accentEnd
        )
        return ExploreUnifiedItem(
            id = zone.id,
            canonicalKey = canonicalKey,
            kind = ExploreUnifiedKind.Mood,
            title = title,
            subtitle = subtitle,
            emoji = zone.emoji,
            query = zone.query,
            accentStart = zone.accentStart,
            accentEnd = zone.accentEnd,
            portraitKey = spec.portraitKey,
            zone = zone,
            providerCategory = provider
        )
    }

    val coreMoods = listOfNotNull(
        buildMoodItem(canonicalKey = "lofi-chill", moodId = "chill", fallbackZoneId = "lofi-chill"),
        buildMoodItem(canonicalKey = "mood-workout", moodId = "gym"),
        buildMoodItem(canonicalKey = "mood-focus", moodId = "focus"),
        buildMoodItem(canonicalKey = "mood-party", moodId = "party"),
        buildMoodItem(canonicalKey = "mood-drive", moodId = "drive"),
        buildMoodItem(canonicalKey = "mood-sad", moodId = "sad")
    )

    val extraMatchedMoods = CanonicalMoodSpecs
        .filter { spec -> spec.canonicalKey !in setOf("lofi-chill", "mood-workout", "mood-focus", "mood-party", "mood-drive", "mood-sad") }
        .mapNotNull { spec ->
            val provider = matchedProviderByCanonical[spec.canonicalKey] ?: return@mapNotNull null
            val zone = ExploreZone(
                id = spec.canonicalKey,
                label = provider.title,
                emoji = spec.emoji,
                query = "${provider.title} music",
                accentStart = spec.accentStart,
                accentEnd = spec.accentEnd
            )
            ExploreUnifiedItem(
                id = zone.id,
                canonicalKey = spec.canonicalKey,
                kind = ExploreUnifiedKind.Mood,
                title = provider.title,
                subtitle = provider.section.ifBlank { strings.exploreMoodSection },
                emoji = spec.emoji,
                query = zone.query,
                accentStart = spec.accentStart,
                accentEnd = spec.accentEnd,
                portraitKey = spec.portraitKey,
                zone = zone,
                providerCategory = provider
            )
        }

    val dynamicMoods = unmatchedMoodCategories.map { provider ->
        val (startColor, endColor) = exploreCategoryColorPair(provider.params)
        val zoneId = "mood-provider-${provider.params}"
        val zone = ExploreZone(
            id = zoneId,
            label = provider.title,
            emoji = "✨",
            query = "${provider.title} music",
            accentStart = startColor,
            accentEnd = endColor
        )
        ExploreUnifiedItem(
            id = zoneId,
            canonicalKey = normalizeExploreCategoryToken(provider.title).ifBlank { provider.params },
            kind = ExploreUnifiedKind.Mood,
            title = provider.title,
            subtitle = provider.section.ifBlank { strings.exploreMoodSection },
            emoji = "✨",
            query = zone.query,
            accentStart = startColor,
            accentEnd = endColor,
            portraitKey = "lofi-chill",
            zone = zone,
            providerCategory = provider
        )
    }

    val fullMoods = (coreMoods + extraMatchedMoods + dynamicMoods).distinctBy { it.canonicalKey }

    val coreGenreZoneIds = listOf(
        "pop-global",
        "rap-drill",
        "elettronica",
        "rock-alt",
        "rnb-soul",
        "latino",
        "local-wave",
        "anime-jpop"
    )

    val coreGenres = coreGenreZoneIds.mapNotNull { zoneId ->
        val baseZone = zonesById[zoneId] ?: return@mapNotNull null
        val spec = CanonicalGenreSpecs.firstOrNull { it.canonicalKey == zoneId }
        val provider = matchedProviderByCanonical[zoneId]
        ExploreUnifiedItem(
            id = baseZone.id,
            canonicalKey = zoneId,
            kind = ExploreUnifiedKind.Genre,
            title = baseZone.label,
            subtitle = strings.genres,
            emoji = baseZone.emoji,
            query = baseZone.query,
            accentStart = baseZone.accentStart,
            accentEnd = baseZone.accentEnd,
            portraitKey = spec?.portraitKey ?: zoneId,
            zone = baseZone,
            providerCategory = provider
        )
    }

    val extraMatchedGenres = CanonicalGenreSpecs
        .filter { spec -> spec.canonicalKey !in coreGenreZoneIds }
        .mapNotNull { spec ->
            val provider = matchedProviderByCanonical[spec.canonicalKey] ?: return@mapNotNull null
            val query = "${provider.title} ${contentLocale.homeQueries.firstOrNull().orEmpty()}".trim()
            val zone = ExploreZone(
                id = spec.canonicalKey,
                label = provider.title,
                emoji = spec.emoji,
                query = query,
                accentStart = spec.accentStart,
                accentEnd = spec.accentEnd
            )
            ExploreUnifiedItem(
                id = zone.id,
                canonicalKey = spec.canonicalKey,
                kind = ExploreUnifiedKind.Genre,
                title = provider.title,
                subtitle = provider.section.ifBlank { strings.genres },
                emoji = spec.emoji,
                query = zone.query,
                accentStart = spec.accentStart,
                accentEnd = spec.accentEnd,
                portraitKey = spec.portraitKey,
                zone = zone,
                providerCategory = provider
            )
        }

    val dynamicGenres = unmatchedGenreCategories.map { provider ->
        val (startColor, endColor) = exploreCategoryColorPair(provider.params)
        val zoneId = "genre-provider-${provider.params}"
        val zone = ExploreZone(
            id = zoneId,
            label = provider.title,
            emoji = "🎵",
            query = "${provider.title} music",
            accentStart = startColor,
            accentEnd = endColor
        )
        ExploreUnifiedItem(
            id = zoneId,
            canonicalKey = normalizeExploreCategoryToken(provider.title).ifBlank { provider.params },
            kind = ExploreUnifiedKind.Genre,
            title = provider.title,
            subtitle = provider.section.ifBlank { strings.genres },
            emoji = "🎵",
            query = zone.query,
            accentStart = startColor,
            accentEnd = endColor,
            portraitKey = "pop-global",
            zone = zone,
            providerCategory = provider
        )
    }

    val fullGenres = (coreGenres + extraMatchedGenres + dynamicGenres).distinctBy { it.canonicalKey }

    val allZones = buildList {
        addAll(zones)
        fullMoods.forEach { item -> if (none { it.id == item.zone.id }) add(item.zone) }
        fullGenres.forEach { item -> if (none { it.id == item.zone.id }) add(item.zone) }
    }

    return ExploreUnifiedCatalog(
        fullMoods = fullMoods,
        featuredMoods = fullMoods.take(ExploreFeaturedMoodLimit),
        fullGenres = fullGenres,
        featuredGenres = fullGenres.take(ExploreFeaturedGenreLimit),
        allZones = allZones
    )
}

private val FallbackCategoryColorPairs = listOf(
    0xFF00E5FF.toInt() to 0xFF2979FF.toInt(),
    0xFF9D4EDD.toInt() to 0xFF7C4DFF.toInt(),
    0xFFFF4081.toInt() to 0xFF7C4DFF.toInt(),
    0xFF11998E.toInt() to 0xFF38EF7D.toInt(),
    0xFFFF6E40.toInt() to 0xFFFF1744.toInt(),
    0xFFB388FF.toInt() to 0xFFFF4081.toInt(),
    0xFFFFC400.toInt() to 0xFFFF6E40.toInt(),
    0xFF6A11CB.toInt() to 0xFF2575FC.toInt()
)

internal fun exploreCategoryColorPair(identity: String): Pair<Int, Int> {
    val index = (identity.hashCode() and Int.MAX_VALUE) % FallbackCategoryColorPairs.size
    return FallbackCategoryColorPairs[index]
}
