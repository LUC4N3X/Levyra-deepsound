package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import com.luc4n3x.levyra.nexus.playlistimport.ImportTransliteration
import com.luc4n3x.levyra.data.RecordingIdentityMatch
import com.luc4n3x.levyra.data.addsDifferentPlaybackVariant
import com.luc4n3x.levyra.data.VideoUploadEvidence
import com.luc4n3x.levyra.data.recordingIdentityMatch
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicVideoType
import com.luc4n3x.levyra.domain.videoViewCountBonus

private val PLAYBACK_TITLE_PRODUCTION_MARKER = Regex(
    """[(\[][^)\]]*\b(official|ufficiale|oficial|lyric|lyrics|testo|audio|video|visualizer|visualiser|clip)\b[^)\]]*[)\]]""",
    RegexOption.IGNORE_CASE
)
private val PLAYBACK_TITLE_FEATURE_GROUP = Regex(
    """[(\[]\s*(feat|ft|featuring|con|with)\b\.?[^)\]]*[)\]]""",
    RegexOption.IGNORE_CASE
)
private val PLAYBACK_TITLE_FEATURE_TAIL = Regex(
    """\s(feat|ft|featuring)\b\.?\s.*$""",
    RegexOption.IGNORE_CASE
)
private val PLAYBACK_TITLE_TRAILING_MARKER = Regex(
    """\s[-–|]\s(official\s+)?(music\s+)?(video|audio|lyrics?\s+video)\s*$""",
    RegexOption.IGNORE_CASE
)

internal fun playbackTitleKey(title: String): String {
    val core = title
        .replace(PLAYBACK_TITLE_PRODUCTION_MARKER, " ")
        .replace(PLAYBACK_TITLE_FEATURE_GROUP, " ")
        .replace(PLAYBACK_TITLE_FEATURE_TAIL, " ")
        .replace(PLAYBACK_TITLE_TRAILING_MARKER, " ")
    return playbackTextKey(core).ifBlank { playbackTextKey(title) }
}

internal fun isPlaybackCandidateCompatible(target: Track, candidate: Track): Boolean {
    when (recordingIdentityMatch(target.isrc, candidate.isrc)) {
        RecordingIdentityMatch.Exact -> return true
        RecordingIdentityMatch.Conflict -> return false
        RecordingIdentityMatch.Unknown -> Unit
    }
    if (!playbackTitlesCompatible(target.title, candidate.title)) return false

    val targetArtistTokens = playbackArtistTokens(target.artist)
    if (targetArtistTokens.isEmpty()) return true
    if (target.artistBrowseIds.isNotEmpty() && candidate.artistBrowseIds.any { it in target.artistBrowseIds }) {
        return true
    }
    val candidateArtistTokens = PlaybackTokenSet(playbackArtistTokens("${candidate.artist} ${candidate.title}"))
    val artistMatches = targetArtistTokens.count(candidateArtistTokens::matches)
    val requiredMatches = maxOf(1, (targetArtistTokens.size + 1) / 2)
    return artistMatches >= requiredMatches
}

private fun playbackTitlesCompatible(originalTitle: String, replacementTitle: String): Boolean {
    if (addsDifferentPlaybackVariant(originalTitle, replacementTitle)) return false
    val targetTitle = playbackTitleKey(originalTitle)
    val candidateTitle = playbackTitleKey(replacementTitle)
    if (targetTitle.isBlank() || candidateTitle.isBlank()) return false

    val targetTitleTokens = playbackTokens(targetTitle)
    val candidateTitleTokens = PlaybackTokenSet(playbackTokens(candidateTitle))
    val titleMatches = targetTitleTokens.count(candidateTitleTokens::matches)
    val titleCoverage = if (targetTitleTokens.isEmpty()) 0.0 else titleMatches.toDouble() / targetTitleTokens.size.toDouble()
    return candidateTitle == targetTitle ||
        candidateTitle.startsWith("$targetTitle ") ||
        targetTitle.startsWith("$candidateTitle ") ||
        titleCoverage >= 0.8
}

internal fun playbackCandidateScore(
    target: Track,
    candidate: Track,
    scoreDuration: Boolean = true
): Int {
    when (recordingIdentityMatch(target.isrc, candidate.isrc)) {
        RecordingIdentityMatch.Exact -> return 10_000
        RecordingIdentityMatch.Conflict -> return Int.MIN_VALUE
        RecordingIdentityMatch.Unknown -> Unit
    }
    val targetTitle = playbackTextKey(target.title)
    val targetArtist = playbackTextKey(target.artist)
    val candidateTitle = playbackTextKey(candidate.title)
    val candidateArtist = playbackTextKey(candidate.artist)
    val candidateBlob = playbackTextKey("${candidate.title} ${candidate.artist} ${candidate.album}")
    val titleTokens = playbackTokens(targetTitle)
    val artistTokens = playbackArtistTokens(target.artist)
    var score = 0
    score += playbackTitleScore(targetTitle, candidateTitle, titleTokens, candidateBlob)
    score += playbackArtistScore(targetArtist, candidateArtist, artistTokens, candidateBlob)
    if (PLAYBACK_PENALTY_TERMS.any { candidateBlob.contains(it) } && PLAYBACK_PENALTY_TERMS.none { targetTitle.contains(it) }) score -= 60
    if (candidate.source.contains("YouTube Music", ignoreCase = true)) score += 18
    if (candidate.source.contains("Extractor", ignoreCase = true)) score += 8
    if (scoreDuration) score += playbackDurationScore(target, candidate)
    return score
}

private fun playbackTitleScore(targetTitle: String, candidateTitle: String, titleTokens: List<String>, candidateBlob: String): Int =
    when {
        candidateTitle == targetTitle -> 140
        ImportTransliteration.sameRomanized(targetTitle, candidateTitle) -> 120
        candidateTitle.isNotBlank() && (candidateTitle.contains(targetTitle) || targetTitle.contains(candidateTitle)) -> 95
        titleTokens.isNotEmpty() && titleTokens.all { candidateBlob.split(' ').contains(it) } -> 70
        else -> 0
    }

private fun playbackArtistScore(targetArtist: String, candidateArtist: String, artistTokens: List<String>, candidateBlob: String): Int =
    when {
        targetArtist.isNotBlank() && candidateArtist == targetArtist -> 80
        targetArtist.isNotBlank() && ImportTransliteration.sameRomanized(targetArtist, candidateArtist) -> 70
        targetArtist.isNotBlank() && candidateArtist.isNotBlank() &&
            (candidateArtist.contains(targetArtist) || targetArtist.contains(candidateArtist)) -> 55
        artistTokens.isNotEmpty() && artistTokens.all { candidateBlob.split(' ').contains(it) } -> 35
        else -> 0
    }

private fun playbackDurationScore(target: Track, candidate: Track): Int {
    if (candidate.durationMs <= 0L || target.durationMs <= 0L) return 0
    val delta = kotlin.math.abs(candidate.durationMs - target.durationMs)
    return when {
        delta <= 5_000L -> 30
        delta > 35_000L -> -25
        else -> 0
    }
}

internal val PLAYBACK_AUDIO_ONLY_MARKER = Regex(
    """[(\[]\s*(official\s+|full\s+)?(audio|lyrics?\s+video|lyrics?|testo|visualizer|visualiser)\s*[)\]]""" +
        """|\bofficial\s+audio\b|\baudio\s+ufficiale\b""",
    RegexOption.IGNORE_CASE
)

internal const val VIDEO_AUDIO_ONLY_PENALTY = 10_000

internal val PLAYBACK_FAN_UPLOAD_MARKER = Regex(
    """\b(karaoke|legendad[oa]|tradu[cç][aã]o|traducida|traduzione|subtitulad[oa]|sub\s+espa[nñ]ol|letra|lyrics?|cover|reaction)\b""",
    RegexOption.IGNORE_CASE
)

internal fun videoPlaybackCandidateScore(
    target: Track,
    candidate: Track,
    evidence: VideoUploadEvidence? = null
): Int {
    val recordingScore = playbackCandidateScore(target, candidate, scoreDuration = false)
    if (recordingScore == Int.MIN_VALUE) return Int.MIN_VALUE
    val type = candidate.videoType
    val channelBonus = uploaderChannelBonus(target, evidence)
    val officialUpload = YoutubeMusicVideoType.isOfficialVideo(type) || channelBonus >= VIDEO_OFFICIAL_CHANNEL_BONUS
    val videoPreference = when {
        officialUpload -> 20_000
        YoutubeMusicVideoType.isArtTrack(type) -> -20_000
        YoutubeMusicVideoType.isVideo(type) -> 1_000
        else -> 0
    }
    val officialChannel = target.artistBrowseIds.isNotEmpty() &&
        candidate.artistBrowseIds.any { it in target.artistBrowseIds }
    val uploadTitles = listOfNotNull(candidate.title, evidence?.title)
    val audioOnlyUpload = uploadTitles.any(PLAYBACK_AUDIO_ONLY_MARKER::containsMatchIn)
    val fanUpload = uploadTitles.any(::isUnofficialUploadTitle) && !isUnofficialUploadTitle(target.title)
    val official = officialUpload || channelBonus > 0
    val durationMs = evidence?.durationMs?.takeIf { it > 0L } ?: candidate.durationMs
    val views = maxOf(candidate.youtubeViewCount, evidence?.viewCount ?: -1L)
    return recordingScore + videoPreference + channelBonus +
        (if (officialChannel) 6_000 else 0) -
        (if (audioOnlyUpload) VIDEO_AUDIO_ONLY_PENALTY else 0) -
        (if (fanUpload) VIDEO_AUDIO_ONLY_PENALTY else 0) -
        videoDurationPenalty(target.durationMs, durationMs, official) +
        videoViewCountBonus(views)
}

internal fun Track.forModeResolution(): Track =
    copy(streamUrl = "", videoStreamUrl = "", playbackManifest = null)

internal fun Track.withPreservedAudioIdentity(source: Track): Track {
    if (audioVideoId.isNotBlank()) return this
    val audioId = source.audioVideoId.trim().ifBlank {
        youtubePlayableTrack(source)?.audioVideoId?.trim().orEmpty()
    }
    return if (audioId.isBlank()) this else copy(audioVideoId = audioId)
}

internal fun videoCandidateId(candidate: Track): String =
    youtubeVideoId(candidate.videoUrl).ifBlank { candidate.id.trim() }

internal const val VIDEO_PAIRING_AUTHORITY_BONUS = 20_000

internal const val VIDEO_PROVIDER_RANK_STEP = 200

internal fun selectPreferredVideoPlaybackCandidate(
    target: Track,
    candidates: List<Track>,
    authoritativeIds: Set<String> = emptySet(),
    evidence: Map<String, VideoUploadEvidence> = emptyMap()
): Track? = rankVideoPlaybackCandidates(target, candidates, authoritativeIds, evidence).firstOrNull()

internal fun rankVideoPlaybackCandidates(
    target: Track,
    candidates: List<Track>,
    authoritativeIds: Set<String> = emptySet(),
    evidence: Map<String, VideoUploadEvidence> = emptyMap()
): List<Track> {
    val videoPrimary = YoutubeMusicVideoType.isVideo(target.videoType)
    val audioIdentityIds = buildSet {
        add(target.audioVideoId.trim())
        if (!videoPrimary) {
            add(target.id.trim())
            add(youtubeVideoId(target.videoUrl).trim())
        }
    }.filter { YOUTUBE_PLAYABLE_VIDEO_ID.matches(it) }.toSet() - target.counterpartVideoId.trim()
    val eligible = candidates.asSequence()
        .mapIndexed { rank, candidate -> rank to candidate }
        .filter { (_, candidate) ->
            val candidateId = videoCandidateId(candidate)
            YOUTUBE_PLAYABLE_VIDEO_ID.matches(candidateId) &&
                candidateId !in audioIdentityIds &&
                !YoutubeMusicVideoType.isArtTrack(candidate.videoType)
        }
        .filter { (_, candidate) ->
            val upload = evidence[videoCandidateId(candidate)]
            isPlaybackCandidateCompatible(target, candidate) &&
                upload?.let { !addsDifferentPlaybackVariant(target.title, it.title) } != false &&
                !isVideoDurationOutlier(target.durationMs, upload?.durationMs?.takeIf { it > 0L } ?: candidate.durationMs)
        }
        .distinctBy { (_, candidate) -> videoCandidateId(candidate) }
        .toList()
    val leaderId = dominantViewCountLeader(eligible.map { it.second }, evidence)
    return eligible
        .map { (rank, candidate) ->
            val candidateId = videoCandidateId(candidate)
            val authority = if (
                candidateId in authoritativeIds && YoutubeMusicVideoType.isVideo(candidate.videoType)
            ) {
                VIDEO_PAIRING_AUTHORITY_BONUS
            } else {
                0
            }
            val popularity = if (candidateId == leaderId) VIDEO_POPULARITY_LEADER_BONUS else 0
            val score = videoPlaybackCandidateScore(target, candidate, evidence[candidateId]) + authority + popularity -
                rank * VIDEO_PROVIDER_RANK_STEP
            score to candidate
        }
        .sortedByDescending { it.first }
        .map { it.second }
}

private val PLAYBACK_PENALTY_TERMS = listOf(
    "karaoke", "cover", "reaction", "sped up", "slowed", "instrumental", "remix", "live", "nightcore",
    "караоке", "кавер", "ремикс", "ремікс", "минусовка", "инструментал", "інструментал", "наживо", "живой звук"
)

internal fun playbackTextKey(value: String): String = MusicIdentityText.normalize(value)

private class PlaybackTokenSet(tokens: List<String>) {
    private val exact = tokens.toHashSet()
    private val romanized = tokens.mapTo(HashSet(), ImportTransliteration::romanizedKey)
    private val romanizedCyrillic = tokens.filter(ImportTransliteration::containsCyrillic)
        .mapTo(HashSet(), ImportTransliteration::romanizedKey)

    fun matches(token: String): Boolean {
        if (token in exact) return true
        val key = ImportTransliteration.romanizedKey(token)
        if (key.length < 2) return false
        return if (ImportTransliteration.containsCyrillic(token)) key in romanized else key in romanizedCyrillic
    }
}

private fun playbackTokens(value: String): List<String> {
    return playbackTextKey(value).split(' ').filter { it.length >= 2 }
}

internal fun playbackArtistTokens(value: String): List<String> {
    val ignored = setOf("feat", "featuring", "ft", "and", "the", "con", "with", "vs")
    return playbackTokens(value).filterNot { it in ignored }
}
