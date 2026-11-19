package com.luc4n3x.levyra.data.storypost

import java.util.Locale

enum class StoryPostFileKind { Image, Audio, Video, Caption }

enum class StoryPostKind(val storageValue: String) {
    Photos("photos"),
    Video("video");

    companion object {
        fun fromStorage(value: String?): StoryPostKind? = entries.firstOrNull { it.storageValue == value }
    }
}

data class StoryPostSourceFile(
    val path: String,
    val sizeBytes: Long,
    val previewModel: String
) {
    val name: String get() = path.substringAfterLast('/')
    val kind: StoryPostFileKind? get() = storyPostFileKind(name)
}

enum class StoryPostPlanProblem { NoMedia, NoAudio }

data class StoryPostPlan(
    val kind: StoryPostKind?,
    val images: List<StoryPostSourceFile>,
    val audioCandidates: List<StoryPostSourceFile>,
    val videoCandidates: List<StoryPostSourceFile>,
    val captionCandidates: List<StoryPostSourceFile>,
    val problem: StoryPostPlanProblem?
) {
    val defaultAudio: StoryPostSourceFile? get() = audioCandidates.firstOrNull()
    val defaultVideo: StoryPostSourceFile? get() = videoCandidates.firstOrNull()
    val defaultCaption: StoryPostSourceFile? get() = captionCandidates.firstOrNull()
    val needsAudioChoice: Boolean get() = kind == StoryPostKind.Photos && audioCandidates.size > 1
    val needsVideoChoice: Boolean get() = kind == StoryPostKind.Video && videoCandidates.size > 1
    val needsCaptionChoice: Boolean get() = captionCandidates.size > 1
}

fun planStoryPost(files: List<StoryPostSourceFile>): StoryPostPlan {
    val visible = files.filterNot { isIgnoredStoryPostPath(it.path) }
    val byKind = visible.groupBy { it.kind }
    val images = byKind[StoryPostFileKind.Image].orEmpty().sortedWith(storyPostFileOrder)
    val audio = byKind[StoryPostFileKind.Audio].orEmpty().sortedWith(storyPostFileOrder)
    val videos = byKind[StoryPostFileKind.Video].orEmpty().sortedWith(storyPostFileOrder)
    val captions = byKind[StoryPostFileKind.Caption].orEmpty()
        .sortedWith(compareBy<StoryPostSourceFile> { captionRank(it.name) }.then(storyPostFileOrder))
    val kind = when {
        images.isNotEmpty() && audio.isNotEmpty() -> StoryPostKind.Photos
        videos.isNotEmpty() -> StoryPostKind.Video
        else -> null
    }
    val problem = when {
        kind != null -> null
        images.isNotEmpty() -> StoryPostPlanProblem.NoAudio
        else -> StoryPostPlanProblem.NoMedia
    }
    return StoryPostPlan(kind, images, audio, videos, captions, problem)
}

fun storyPostFileKind(name: String): StoryPostFileKind? {
    val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when (extension) {
        in IMAGE_EXTENSIONS -> StoryPostFileKind.Image
        in AUDIO_EXTENSIONS -> StoryPostFileKind.Audio
        in VIDEO_EXTENSIONS -> StoryPostFileKind.Video
        in CAPTION_EXTENSIONS -> StoryPostFileKind.Caption
        else -> null
    }
}

fun isIgnoredStoryPostPath(path: String): Boolean {
    val segments = path.split('/').filter(String::isNotEmpty)
    return segments.any { it.startsWith('.') || it.equals("__MACOSX", ignoreCase = true) }
}

val storyPostFileOrder: Comparator<StoryPostSourceFile> = Comparator { left, right ->
    compareNatural(left.path, right.path)
}

fun compareNatural(left: String, right: String): Int {
    var i = 0
    var j = 0
    while (i < left.length && j < right.length) {
        val a = left[i]
        val b = right[j]
        if (a.isDigit() && b.isDigit()) {
            val startA = i
            val startB = j
            while (i < left.length && left[i].isDigit()) i++
            while (j < right.length && right[j].isDigit()) j++
            val digitsA = left.substring(startA, i).trimStart('0')
            val digitsB = right.substring(startB, j).trimStart('0')
            if (digitsA.length != digitsB.length) return digitsA.length - digitsB.length
            val numeric = digitsA.compareTo(digitsB)
            if (numeric != 0) return numeric
        } else {
            val folded = a.lowercaseChar().compareTo(b.lowercaseChar())
            if (folded != 0) return folded
            i++
            j++
        }
    }
    val remaining = left.length - i - (right.length - j)
    return if (remaining != 0) remaining else left.compareTo(right)
}

fun normalizeStoryPostCaption(raw: String): String = raw
    .removePrefix("\uFEFF")
    .replace("\r\n", "\n")
    .replace('\r', '\n')
    .trim()
    .take(MAX_CAPTION_CHARS)

fun storyPostTitle(caption: String, sourceName: String): String {
    val firstLine = caption.lineSequence().map(String::trim).firstOrNull(String::isNotEmpty).orEmpty()
    val base = firstLine.ifEmpty { sourceName.substringBeforeLast('.').ifBlank { sourceName } }
    return if (base.length <= MAX_TITLE_CHARS) base else base.take(MAX_TITLE_CHARS - 1).trimEnd() + "…"
}

data class StoryPostZipEntry(val name: String, val declaredSize: Long, val isDirectory: Boolean)

data class StoryPostLimits(
    val maxEntries: Int = 400,
    val maxEntryBytes: Long = 1L shl 30,
    val maxTotalBytes: Long = 2L shl 30,
    val maxCaptionBytes: Long = 256L * 1024L
)

sealed interface StoryPostZipCheck {
    data class Accepted(val files: List<StoryPostZipEntry>) : StoryPostZipCheck
    data object UnsafePath : StoryPostZipCheck
    data object TooLarge : StoryPostZipCheck
}

fun checkStoryPostZipEntries(entries: List<StoryPostZipEntry>, limits: StoryPostLimits = StoryPostLimits()): StoryPostZipCheck {
    if (entries.size > limits.maxEntries) return StoryPostZipCheck.TooLarge
    if (entries.any { !isSafeZipEntryName(it.name) }) return StoryPostZipCheck.UnsafePath
    val wanted = entries.filter { entry ->
        !entry.isDirectory && !isIgnoredStoryPostPath(entry.name) && storyPostFileKind(entry.name) != null
    }
    if (wanted.map { it.name.lowercase(Locale.ROOT) }.toSet().size != wanted.size) return StoryPostZipCheck.UnsafePath
    var total = 0L
    for (entry in wanted) {
        val limit = if (storyPostFileKind(entry.name) == StoryPostFileKind.Caption) limits.maxCaptionBytes else limits.maxEntryBytes
        if (entry.declaredSize < 0L || entry.declaredSize > limit) return StoryPostZipCheck.TooLarge
        total += entry.declaredSize
        if (total > limits.maxTotalBytes) return StoryPostZipCheck.TooLarge
    }
    return StoryPostZipCheck.Accepted(wanted)
}

fun isSafeZipEntryName(name: String): Boolean {
    if (name.isEmpty() || name.length > MAX_ENTRY_NAME_CHARS) return false
    if (name.any { it == '\u0000' || it == '\\' || it == ':' }) return false
    if (name.startsWith('/')) return false
    return name.split('/').none { it == ".." || it == "." }
}

fun storyPostStoredName(index: Int, source: StoryPostSourceFile): String {
    val extension = source.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }
        .take(8)
    val prefix = when (source.kind) {
        StoryPostFileKind.Image -> "image-%03d".format(Locale.ROOT, index + 1)
        StoryPostFileKind.Audio -> "audio"
        StoryPostFileKind.Video -> "video"
        StoryPostFileKind.Caption -> "caption"
        null -> "file-$index"
    }
    return if (extension.isEmpty()) prefix else "$prefix.$extension"
}

private fun captionRank(name: String): Int {
    val stem = name.substringBeforeLast('.').lowercase(Locale.ROOT)
    return if (CAPTION_HINTS.any { stem.contains(it) }) 0 else 1
}

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "avif", "bmp")
private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "weba")
private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mov", "webm", "mkv", "3gp")
private val CAPTION_EXTENSIONS = setOf("txt")
private val CAPTION_HINTS = listOf("caption", "desc", "description", "text", "title", "文案", "描述", "标题")
private const val MAX_CAPTION_CHARS = 20_000
private const val MAX_TITLE_CHARS = 80
private const val MAX_ENTRY_NAME_CHARS = 512
