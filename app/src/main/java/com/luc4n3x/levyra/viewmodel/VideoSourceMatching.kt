package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.VideoUploadEvidence
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeMusicVideoType

internal data class VideoSourceDiscovery(
    val ranked: List<Track>,
    val evidence: Map<String, VideoUploadEvidence>
)

internal const val VIDEO_OFFICIAL_CHANNEL_BONUS = 8_000
internal const val VIDEO_LABEL_CHANNEL_BONUS = 3_000
internal const val VIDEO_POPULARITY_LEADER_BONUS = 1_000
internal const val VIDEO_POPULARITY_LEADER_RATIO = 3L
internal const val VIDEO_DURATION_MISMATCH_PENALTY = 1_500
private const val OFFICIAL_VIDEO_SHORTER_TOLERANCE_MS = 15_000L
private const val OFFICIAL_VIDEO_LONGER_TOLERANCE_MS = 150_000L
private const val OFFICIAL_VIDEO_LONGER_TOLERANCE_PERCENT = 60L
private const val UPLOAD_DURATION_TOLERANCE_MS = 10_000L
private const val MODE_SWITCH_END_GUARD_MS = 6_000L
private const val OUTLIER_LONGER_FACTOR = 3L
private const val OUTLIER_LONGER_EXTRA_MS = 600_000L

private val UNOFFICIAL_UPLOAD_MARKER = Regex(
    """\b(lyrical|visuali[sz]er|fan[\s-]?made|8d|slowed|reverb|sped\s+up|nightcore|status\s+video)\b""",
    RegexOption.IGNORE_CASE
)

private val ARTIST_CREDIT_SEPARATOR = Regex(
    """\s*(?:,|&|\+|/|\bfeat\.?|\bft\.?|\bfeaturing\b|\bwith\b|\bx\b|\band\b|\be\b|\by\b|\bи\b)\s*""",
    RegexOption.IGNORE_CASE
)

private val CHANNEL_DECORATION_SUFFIXES = listOf("vevo", "official", "music", "tv")

internal fun isUnofficialUploadTitle(title: String): Boolean =
    PLAYBACK_FAN_UPLOAD_MARKER.containsMatchIn(title) || UNOFFICIAL_UPLOAD_MARKER.containsMatchIn(title)

internal fun uploaderChannelBonus(target: Track, evidence: VideoUploadEvidence?): Int {
    val upload = evidence ?: return 0
    val channel = compactIdentityKey(upload.channelName)
    if (channel.isBlank()) return 0
    val artists = targetArtistKeys(target.artist)
    val owners = upload.channelName.split(ARTIST_CREDIT_SEPARATOR)
        .map { owner -> CHANNEL_DECORATION_SUFFIXES.fold(compactIdentityKey(owner)) { name, suffix -> name.removeSuffix(suffix) } }
    val artistChannel = artists.any { artist -> artist == channel || artist in owners }
    val cleanTitle = !isUnofficialUploadTitle(upload.title) && !PLAYBACK_AUDIO_ONLY_MARKER.containsMatchIn(upload.title)
    val titleCreditsArtist = compactIdentityKey(upload.title).let { title -> artists.any(title::contains) }
    return when {
        !cleanTitle -> 0
        channel.endsWith("vevo") && (upload.channelVerified || upload.officialArtistChannel) -> VIDEO_OFFICIAL_CHANNEL_BONUS
        (upload.officialArtistChannel || upload.channelVerified || owners.size > 1) && artistChannel -> VIDEO_OFFICIAL_CHANNEL_BONUS
        artistChannel && titleCreditsArtist -> VIDEO_LABEL_CHANNEL_BONUS
        upload.channelVerified && titleCreditsArtist -> VIDEO_LABEL_CHANNEL_BONUS
        else -> 0
    }
}

internal fun videoDurationPenalty(targetMs: Long, candidateMs: Long, official: Boolean): Int {
    if (targetMs <= 0L || candidateMs <= 0L) return 0
    val delta = candidateMs - targetMs
    val accepted = if (official) {
        val longer = maxOf(OFFICIAL_VIDEO_LONGER_TOLERANCE_MS, targetMs * OFFICIAL_VIDEO_LONGER_TOLERANCE_PERCENT / 100L)
        -OFFICIAL_VIDEO_SHORTER_TOLERANCE_MS..longer
    } else {
        -UPLOAD_DURATION_TOLERANCE_MS..UPLOAD_DURATION_TOLERANCE_MS
    }
    return if (delta in accepted) 0 else VIDEO_DURATION_MISMATCH_PENALTY
}

internal fun isVideoDurationOutlier(targetMs: Long, candidateMs: Long): Boolean {
    if (targetMs <= 0L || candidateMs <= 0L) return false
    return candidateMs > maxOf(targetMs * OUTLIER_LONGER_FACTOR, targetMs + OUTLIER_LONGER_EXTRA_MS) ||
        candidateMs * 2L < targetMs
}

internal fun dominantViewCountLeader(
    candidates: List<Track>,
    evidence: Map<String, VideoUploadEvidence>
): String? {
    val counted = candidates
        .map { candidate ->
            val id = videoCandidateId(candidate)
            id to maxOf(candidate.youtubeViewCount, evidence[id]?.viewCount ?: -1L)
        }
        .filter { (_, views) -> views > 0L }
        .sortedByDescending { (_, views) -> views }
    if (counted.size < 2) return null
    val (leaderId, leaderViews) = counted[0]
    return leaderId.takeIf { leaderViews >= counted[1].second * VIDEO_POPULARITY_LEADER_RATIO }
}

internal fun modeSwitchPositionMs(positionMs: Long, targetDurationMs: Long): Long {
    val position = positionMs.coerceAtLeast(0L)
    if (targetDurationMs <= 0L) return position
    return position.coerceAtMost((targetDurationMs - MODE_SWITCH_END_GUARD_MS).coerceAtLeast(0L))
}

private fun compactIdentityKey(value: String): String = playbackTextKey(value).replace(" ", "")

private fun targetArtistKeys(artist: String): List<String> =
    artist.split(ARTIST_CREDIT_SEPARATOR)
        .map(::compactIdentityKey)
        .filter { it.length >= 3 }
        .distinct()

internal fun videoSourceOptions(seed: Track, discovery: VideoSourceDiscovery): List<VideoSourceOption> =
    discovery.ranked.map { candidate ->
        val videoId = videoCandidateId(candidate)
        val upload = discovery.evidence[videoId]
        VideoSourceOption(
            videoId = videoId,
            title = upload?.title?.takeIf(String::isNotBlank) ?: candidate.title,
            channel = upload?.channelName?.takeIf(String::isNotBlank) ?: candidate.artist,
            durationMs = upload?.durationMs?.takeIf { it > 0L } ?: candidate.durationMs,
            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg",
            official = isOfficialVideoSource(seed, candidate, upload)
        )
    }

internal fun isOfficialVideoSource(seed: Track, candidate: Track, upload: VideoUploadEvidence?): Boolean {
    val titles = listOfNotNull(candidate.title, upload?.title)
    if (titles.any { isUnofficialUploadTitle(it) || PLAYBACK_AUDIO_ONLY_MARKER.containsMatchIn(it) }) return false
    if (uploaderChannelBonus(seed, upload) >= VIDEO_OFFICIAL_CHANNEL_BONUS) return true
    if (!YoutubeMusicVideoType.isOfficialVideo(candidate.videoType)) return false
    return seed.artistBrowseIds.isEmpty() || candidate.artistBrowseIds.any { it in seed.artistBrowseIds }
}

internal fun videoSourceQueryTitle(title: String): String = playbackTitleKey(title).ifBlank { title.trim() }
