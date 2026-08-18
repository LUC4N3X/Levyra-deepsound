package com.luc4n3x.levyra.feature.sharedmedia

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

data class LevyraSharedTrack(
    val id: String,
    val title: String,
    val artist: String
)

data class LevyraSharedPlaylist(
    val schemaVersion: Int,
    val title: String,
    val tracks: List<LevyraSharedTrack>
)

enum class LevyraPlaylistDecodeError {
    NotLevyraPayload,
    UnsupportedVersion,
    Malformed,
    ChecksumMismatch,
    TooLarge,
    Empty
}

sealed interface LevyraPlaylistDecodeResult {
    data class Success(val playlist: LevyraSharedPlaylist) : LevyraPlaylistDecodeResult
    data class Failure(val error: LevyraPlaylistDecodeError) : LevyraPlaylistDecodeResult
}

const val LEVYRA_SHARE_SCHEME = "levyra"
const val LEVYRA_SHARE_PLAYLIST_HOST = "playlist"

object LevyraPlaylistShareCodec {
    const val SCHEMA_VERSION = 1
    const val MAX_TRACKS = 500
    const val MAX_TITLE_CHARS = 120
    const val MAX_ENCODED_CHARS = 60_000

    private const val MAGIC = "LVP"
    private const val FIELD_SEPARATOR = '\u001F'
    private const val MAX_INFLATED_BYTES = 512 * 1024
    private const val MAX_TRACK_TEXT_CHARS = 90
    private val TRACK_ID = Regex("^[A-Za-z0-9_-]{6,24}$")
    private val CONTROL_CHARS = Regex("\\p{Cntrl}")
    private val LINK_PREFIX = "$LEVYRA_SHARE_SCHEME://$LEVYRA_SHARE_PLAYLIST_HOST"
    private val LINK_PATTERN = Regex("""(?i)levyra://playlist\?\S+""")
    private val PAYLOAD_CHARS = (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('_', '-')).toSet()

    fun encode(title: String, tracks: List<LevyraSharedTrack>): String? {
        val sanitizedTracks = sanitizeTracks(tracks)
        if (sanitizedTracks.isEmpty()) return null
        val body = buildString {
            append(sanitizeTitle(title))
            sanitizedTracks.forEach { track ->
                append('\n')
                append(track.id)
                append(FIELD_SEPARATOR)
                append(track.title)
                append(FIELD_SEPARATOR)
                append(track.artist)
            }
        }
        val checksum = CRC32().apply { update(body.toByteArray(Charsets.UTF_8)) }.value
        val document = "$MAGIC$SCHEMA_VERSION\n${checksum.toString(16)}\n$body"
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(deflate(document.toByteArray(Charsets.UTF_8)))
        return encoded.takeIf { it.length <= MAX_ENCODED_CHARS }
    }

    fun encodeLink(title: String, tracks: List<LevyraSharedTrack>): String? =
        encode(title, tracks)?.let { "$LINK_PREFIX?v=$SCHEMA_VERSION&d=$it" }

    fun extractPayload(rawText: String): String? {
        val link = LINK_PATTERN.find(rawText)?.value ?: return null
        val query = link.substringAfter('?', "")
        if (query.isEmpty()) return null
        return query.split('&')
            .firstOrNull { it.startsWith("d=") }
            ?.removePrefix("d=")
            ?.trimEnd { it !in PAYLOAD_CHARS }
            ?.takeIf { it.isNotBlank() }
    }

    fun decode(encoded: String): LevyraPlaylistDecodeResult {
        if (encoded.isBlank()) return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Empty)
        if (encoded.length > MAX_ENCODED_CHARS) {
            return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.TooLarge)
        }
        val compressed = runCatching { Base64.getUrlDecoder().decode(encoded) }.getOrNull() ?: return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Malformed)
        val document = inflate(compressed)
            ?: return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Malformed)

        val headerEnd = document.indexOf('\n')
        if (headerEnd <= 0) return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Malformed)
        val header = document.substring(0, headerEnd)
        if (!header.startsWith(MAGIC)) {
            return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.NotLevyraPayload)
        }
        val version = header.removePrefix(MAGIC).toIntOrNull()
            ?: return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Malformed)
        if (version != SCHEMA_VERSION) {
            return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.UnsupportedVersion)
        }

        val checksumEnd = document.indexOf('\n', headerEnd + 1)
        if (checksumEnd <= headerEnd) {
            return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Malformed)
        }
        val declaredChecksum = document.substring(headerEnd + 1, checksumEnd).toLongOrNull(16)
            ?: return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Malformed)
        val body = document.substring(checksumEnd + 1)
        val actualChecksum = CRC32().apply { update(body.toByteArray(Charsets.UTF_8)) }.value
        if (actualChecksum != declaredChecksum) {
            return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.ChecksumMismatch)
        }

        val lines = body.split('\n')
        val title = sanitizeTitle(lines.firstOrNull().orEmpty())
        val rawTracks = lines.drop(1).mapNotNull { line ->
            val parts = line.split(FIELD_SEPARATOR)
            val id = parts.getOrNull(0).orEmpty()
            if (id.isBlank()) return@mapNotNull null
            LevyraSharedTrack(
                id = id,
                title = parts.getOrNull(1).orEmpty(),
                artist = parts.getOrNull(2).orEmpty()
            )
        }
        if (rawTracks.size > MAX_TRACKS) {
            return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.TooLarge)
        }
        val tracks = sanitizeTracks(rawTracks)
        if (tracks.isEmpty()) return LevyraPlaylistDecodeResult.Failure(LevyraPlaylistDecodeError.Empty)
        return LevyraPlaylistDecodeResult.Success(
            LevyraSharedPlaylist(schemaVersion = version, title = title, tracks = tracks)
        )
    }

    private fun sanitizeTitle(value: String): String =
        value.replace(CONTROL_CHARS, " ").trim().take(MAX_TITLE_CHARS)

    private fun sanitizeText(value: String): String =
        value.replace(CONTROL_CHARS, " ").trim().take(MAX_TRACK_TEXT_CHARS)

    private fun sanitizeTracks(tracks: List<LevyraSharedTrack>): List<LevyraSharedTrack> = tracks
        .asSequence()
        .map { it.copy(id = it.id.trim()) }
        .filter { TRACK_ID.matches(it.id) }
        .distinctBy { it.id }
        .take(MAX_TRACKS)
        .map { it.copy(title = sanitizeText(it.title), artist = sanitizeText(it.artist)) }
        .toList()

    private fun deflate(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        return try {
            deflater.setInput(input)
            deflater.finish()
            val output = ByteArrayOutputStream(input.size / 2 + 32)
            val buffer = ByteArray(4_096)
            while (!deflater.finished()) {
                val produced = deflater.deflate(buffer)
                if (produced > 0) output.write(buffer, 0, produced)
            }
            output.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun inflate(input: ByteArray): String? {
        val inflater = Inflater()
        return try {
            inflater.setInput(input)
            val output = ByteArrayOutputStream(input.size * 2)
            val buffer = ByteArray(4_096)
            while (!inflater.finished()) {
                val produced = inflater.inflate(buffer)
                if (produced == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
                if (output.size() + produced > MAX_INFLATED_BYTES) return null
                output.write(buffer, 0, produced)
            }
            String(output.toByteArray(), Charsets.UTF_8)
        } catch (_: DataFormatException) {
            null
        } finally {
            inflater.end()
        }
    }
}
