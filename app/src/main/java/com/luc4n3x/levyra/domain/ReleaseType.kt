package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale

enum class ReleaseType {
    Album,
    Single,
    Compilation,
    Ep,
    Unknown
}

val ReleaseType.isFullAlbum: Boolean
    get() = this == ReleaseType.Album

val ReleaseType.isSingleLike: Boolean
    get() = this == ReleaseType.Single || this == ReleaseType.Ep

fun releaseTypeFromProviderLabel(value: String): ReleaseType {
    val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (normalized.isBlank()) return ReleaseType.Unknown
    val tokens = normalized.split(' ').filter(String::isNotBlank).toSet()
    return when {
        normalized == "ep" || "ep" in tokens || normalized.contains("extended play") -> ReleaseType.Ep
        COMPILATION_LABELS.any { label -> normalized == label || label in tokens || normalized.contains(label) } -> ReleaseType.Compilation
        SINGLE_LABELS.any { label -> normalized == label || label in tokens } -> ReleaseType.Single
        ALBUM_LABELS.any { label -> normalized == label || label in tokens } -> ReleaseType.Album
        else -> ReleaseType.Unknown
    }
}

private val ALBUM_LABELS = setOf(
    "album", "albumo", "alben", "albom", "albumes", "albumi", "专辑", "專輯", "アルバム", "앨범", "अल्बम", "อัลบั้ม", "אלבום", "ألبوم"
)

private val SINGLE_LABELS = setOf(
    "single", "singolo", "singoli", "sencillo", "sencillos", "singl", "singel", "單曲", "单曲", "シングル", "싱글", "एकल", "ซิงเกิล", "סינגל", "أغنية منفردة"
)

private val COMPILATION_LABELS = setOf(
    "compilation", "compilations", "raccolta", "raccolte", "anthology", "best of", "greatest hits", "合集", "合輯", "コンピレーション", "컴필레이션", "संकलन", "รวมเพลง", "אוסף", "تجميع"
)
