from pathlib import Path
import re

ROOT = Path('.')


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding='utf-8')


def write(path: str, value: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(value, encoding='utf-8')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one anchor, found {count}')
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, lambda _: replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f'{label}: expected one regex match, found {count}')
    return updated


# ---------------------------------------------------------------------------
# Release identity model
# ---------------------------------------------------------------------------
write(
    'app/src/main/java/com/luc4n3x/levyra/domain/ReleaseType.kt',
    r'''package com.luc4n3x.levyra.domain

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
'''
)

models_path = 'app/src/main/java/com/luc4n3x/levyra/domain/Models.kt'
models = read(models_path)
models = replace_once(
    models,
    '    val explicit: Boolean = false\n)\n\n@Immutable\ndata class ArtistProfile(',
    '    val explicit: Boolean = false,\n    val releaseType: ReleaseType = ReleaseType.Unknown\n)\n\n@Immutable\ndata class ArtistProfile(',
    'artist release type'
)
models = replace_once(
    models,
    '    val videosParams: String = ""\n)\n\n@Immutable\ndata class SearchResults(',
    '    val videosParams: String = "",\n    val compilations: List<ArtistRelease> = emptyList()\n)\n\n@Immutable\ndata class SearchResults(',
    'artist compilations'
)
models = replace_once(
    models,
    '    val metadataConfidence: Int = 0\n)\n\ndata class AlbumDetail(',
    '    val metadataConfidence: Int = 0,\n    val releaseType: ReleaseType = ReleaseType.Unknown\n)\n\ndata class AlbumDetail(',
    'album hit release type'
)
write(models_path, models)

# ---------------------------------------------------------------------------
# Secure YouTube Music cookie parsing and storage
# ---------------------------------------------------------------------------
write(
    'app/src/main/java/com/luc4n3x/levyra/data/security/YoutubeMusicCredentialStore.kt',
    r'''package com.luc4n3x.levyra.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class YoutubeMusicCredential(
    val cookieHeader: String,
    val sapisid: String
) {
    fun authorizationHeader(nowMs: Long, origin: String = YOUTUBE_MUSIC_ORIGIN): String {
        val seconds = nowMs / 1_000L
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$seconds $sapisid $origin".toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "SAPISIDHASH ${seconds}_$digest"
    }
}

internal object YoutubeMusicCookieParser {
    fun parse(rawValue: String): YoutubeMusicCredential? {
        val raw = rawValue.trim()
        if (raw.isBlank() || raw.length > MAX_IMPORT_CHARS) return null
        val cookies = linkedMapOf<String, String>()
        parseJson(raw, cookies)
        parseNetscape(raw, cookies)
        parseHeader(raw, cookies)
        val safe = cookies
            .filterKeys(ALLOWED_COOKIE_NAMES::contains)
            .mapValues { (_, value) -> value.trim() }
            .filterValues { value -> value.isNotBlank() && value.length <= MAX_COOKIE_VALUE_CHARS && value.none(Char::isWhitespace) }
        val sapisid = sequenceOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")
            .mapNotNull(safe::get)
            .firstOrNull()
            ?: return null
        val header = safe.entries
            .sortedBy { it.key.lowercase(Locale.ROOT) }
            .joinToString("; ") { (name, value) -> "$name=$value" }
        return header.takeIf(String::isNotBlank)?.let { YoutubeMusicCredential(it, sapisid) }
    }

    private fun parseJson(raw: String, output: MutableMap<String, String>) {
        if (!raw.startsWith('{')) return
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return
        root.keys().forEach { key ->
            val value = root.optString(key).trim()
            if (key in ALLOWED_COOKIE_NAMES && value.isNotBlank()) output[key] = value
        }
    }

    private fun parseNetscape(raw: String, output: MutableMap<String, String>) {
        raw.lineSequence().forEach { line ->
            val clean = line.trim()
            if (clean.isBlank() || clean.startsWith('#')) return@forEach
            val columns = clean.split('\t')
            if (columns.size < 7) return@forEach
            val domain = columns[0].removePrefix("#HttpOnly_").lowercase(Locale.ROOT)
            if (domain != ".youtube.com" && domain != "youtube.com" && domain != "music.youtube.com") return@forEach
            val name = columns[5].trim()
            val value = columns[6].trim()
            if (name in ALLOWED_COOKIE_NAMES && value.isNotBlank()) output[name] = value
        }
    }

    private fun parseHeader(raw: String, output: MutableMap<String, String>) {
        val header = raw.substringAfter("Cookie:", raw).replace('\n', ';').replace('\r', ';')
        header.split(';').forEach { segment ->
            val index = segment.indexOf('=')
            if (index <= 0) return@forEach
            val name = segment.substring(0, index).trim()
            val value = segment.substring(index + 1).trim()
            if (name in ALLOWED_COOKIE_NAMES && value.isNotBlank()) output[name] = value
        }
    }

    private const val MAX_IMPORT_CHARS = 64 * 1024
    private const val MAX_COOKIE_VALUE_CHARS = 8 * 1024
    private val ALLOWED_COOKIE_NAMES = setOf(
        "SAPISID", "APISID", "SID", "HSID", "SSID", "LOGIN_INFO", "PREF", "YSC",
        "VISITOR_INFO1_LIVE", "VISITOR_PRIVACY_METADATA", "SOCS", "CONSENT",
        "__Secure-1PAPISID", "__Secure-3PAPISID", "__Secure-1PSID", "__Secure-3PSID",
        "__Secure-1PSIDTS", "__Secure-3PSIDTS", "__Secure-1PSIDCC", "__Secure-3PSIDCC"
    )
}

internal class YoutubeMusicCredentialStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))

    @Synchronized
    fun save(rawValue: String): Boolean {
        val credential = YoutubeMusicCookieParser.parse(rawValue) ?: return false
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(credential.cookieHeader.toByteArray(Charsets.UTF_8))
        val payload = JSONObject()
            .put("version", 1)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()
            .toByteArray(Charsets.UTF_8)
        val stream = runCatching { file.startWrite() }.getOrNull() ?: return false
        return try {
            stream.write(payload)
            stream.fd.sync()
            file.finishWrite(stream)
            true
        } catch (_: Throwable) {
            file.failWrite(stream)
            false
        }
    }

    @Synchronized
    fun load(): YoutubeMusicCredential? {
        val bytes = runCatching { file.openRead().use { input -> input.readBytes(MAX_FILE_BYTES + 1) } }.getOrNull() ?: return null
        if (bytes.isEmpty() || bytes.size > MAX_FILE_BYTES) return null
        val root = runCatching { JSONObject(bytes.toString(Charsets.UTF_8)) }.getOrNull() ?: return null
        val iv = runCatching { Base64.decode(root.optString("iv"), Base64.NO_WRAP) }.getOrNull() ?: return null
        val encrypted = runCatching { Base64.decode(root.optString("data"), Base64.NO_WRAP) }.getOrNull() ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val decrypted = runCatching {
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull() ?: return null
        return YoutubeMusicCookieParser.parse(decrypted)
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    fun hasCredential(): Boolean = load() != null

    fun version(): Long = file.baseFile.lastModified()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE_NAME = "youtube_music_session.enc"
        const val KEY_ALIAS = "levyra.youtube.music.session.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val MAX_FILE_BYTES = 96 * 1024
    }
}

internal const val YOUTUBE_MUSIC_ORIGIN = "https://music.youtube.com"
'''
)

# ---------------------------------------------------------------------------
# Authenticated InnerTube transport, isolated to WEB_REMIX requests
# ---------------------------------------------------------------------------
resilience_path = 'app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicResilienceClient.kt'
resilience = read(resilience_path)
resilience = replace_once(
    resilience,
    'import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders\n',
    'import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders\nimport com.luc4n3x.levyra.data.security.YOUTUBE_MUSIC_ORIGIN\nimport com.luc4n3x.levyra.data.security.YoutubeMusicCredentialStore\n',
    'resilience credential imports'
)
resilience = replace_once(
    resilience,
    '    private val transport = transport ?: OkHttpYoutubeMusicTransport(context)\n',
    '    private val credentialStore = context?.applicationContext?.let(::YoutubeMusicCredentialStore)\n    private val transport = transport ?: OkHttpYoutubeMusicTransport(context, credentialStore)\n',
    'resilience credential store'
)
resilience = replace_once(
    resilience,
    '    internal fun diagnostics(): Map<String, YoutubeMusicClientHealth> = health.toMap()\n',
    '''    fun hasAuthenticatedSession(): Boolean = credentialStore?.hasCredential() == true

    fun importAuthenticatedSession(rawValue: String): Boolean {
        val saved = credentialStore?.save(rawValue) == true
        if (saved) clearResponseCache()
        return saved
    }

    fun clearAuthenticatedSession() {
        credentialStore?.clear()
        clearResponseCache()
    }

    internal fun diagnostics(): Map<String, YoutubeMusicClientHealth> = health.toMap()
''',
    'resilience auth API'
)
resilience = replace_once(
    resilience,
    '        val requestKey = listOf(kind.name, languageCode, browseId, params, continuation, query).joinToString("\\u001f")\n',
    '        val requestKey = listOf(kind.name, languageCode, browseId, params, continuation, query, credentialStore?.version().toString()).joinToString("\\u001f")\n',
    'auth-aware cache key'
)
resilience = replace_once(
    resilience,
    '    private fun cached(key: String): JSONObject? = synchronized(cacheLock) {\n',
    '''    private fun clearResponseCache() = synchronized(cacheLock) {
        responseCache.clear()
        responseCacheBytes = 0L
        visitorData.clear()
    }

    private fun cached(key: String): JSONObject? = synchronized(cacheLock) {
''',
    'clear response cache'
)
resilience = replace_once(
    resilience,
    'private class OkHttpYoutubeMusicTransport(context: Context?) : YoutubeMusicTransport {\n',
    'private class OkHttpYoutubeMusicTransport(\n    context: Context?,\n    private val credentialStore: YoutubeMusicCredentialStore?\n) : YoutubeMusicTransport {\n',
    'transport credential constructor'
)
resilience = replace_once(
    resilience,
    '        if (request.profile.origin.isNotBlank()) builder.header("Origin", request.profile.origin)\n        if (request.profile.androidSdkVersion > 0) builder.header("X-Goog-Api-Format-Version", "2")\n',
    '''        if (request.profile.origin.isNotBlank()) builder.header("Origin", request.profile.origin)
        if (request.profile.id == "web-remix" && request.profile.origin == YOUTUBE_MUSIC_ORIGIN) {
            credentialStore?.load()?.let { credential ->
                builder.header("Cookie", credential.cookieHeader)
                builder.header("Authorization", credential.authorizationHeader(System.currentTimeMillis()))
                builder.header("X-Goog-AuthUser", "0")
            }
        }
        if (request.profile.androidSdkVersion > 0) builder.header("X-Goog-Api-Format-Version", "2")
''',
    'authenticated transport headers'
)
write(resilience_path, resilience)

# ---------------------------------------------------------------------------
# Repository release classification and auth facade
# ---------------------------------------------------------------------------
yt_path = 'app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt'
yt = read(yt_path)
yt = replace_once(
    yt,
    'import com.luc4n3x.levyra.domain.SearchResults\n',
    'import com.luc4n3x.levyra.domain.SearchResults\nimport com.luc4n3x.levyra.domain.ReleaseType\nimport com.luc4n3x.levyra.domain.releaseTypeFromProviderLabel\n',
    'youtube release imports'
)
yt = replace_once(
    yt,
    'internal fun levyraIsAlbumLabel(token: String): Boolean {\n    return token.trim().lowercase(Locale.ROOT) in LEVYRA_LOCALIZED_ALBUM_LABELS\n}\n',
    '''internal fun levyraReleaseType(token: String): ReleaseType = releaseTypeFromProviderLabel(token)

internal fun levyraIsAlbumLabel(token: String): Boolean {
    return levyraReleaseType(token) == ReleaseType.Album
}
''',
    'youtube release classifier'
)
yt = replace_once(
    yt,
    '    private val albumDescriptionRepository = AlbumDescriptionRepository(context)\n',
    '''    private val albumDescriptionRepository = AlbumDescriptionRepository(context)

    fun hasYoutubeMusicSession(): Boolean = resilienceClient.hasAuthenticatedSession()

    fun importYoutubeMusicSession(rawValue: String): Boolean = resilienceClient.importAuthenticatedSession(rawValue)

    fun clearYoutubeMusicSession() = resilienceClient.clearAuthenticatedSession()
''',
    'youtube auth facade'
)
yt = replace_once(
    yt,
    '                isAlbumLabel(kind) && isPlausibleYoutubeMusicAlbumTitle(title) -> {\n',
    '                levyraReleaseType(kind) == ReleaseType.Album && isPlausibleYoutubeMusicAlbumTitle(title) -> {\n',
    'search only full albums'
)
yt = replace_once(
    yt,
    '                            artistBrowseId = artistReference?.browseId.orEmpty()\n                        )\n',
    '                            artistBrowseId = artistReference?.browseId.orEmpty(),\n                            releaseType = ReleaseType.Album\n                        )\n',
    'search album type'
)
yt = replace_once(
    yt,
    '            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }\n            .filter { it.browseId.isNotBlank() || it.query.isNotBlank() }\n            .distinctBy(::albumRecommendationDeduplicationKey)\n            .toList()\n',
    '            .filter { it.releaseType == ReleaseType.Album }\n            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }\n            .filter { it.browseId.isNotBlank() || it.query.isNotBlank() }\n            .distinctBy(::albumRecommendationDeduplicationKey)\n            .toList()\n',
    'base album filter'
)
yt = replace_once(
    yt,
    '        (baseAlbums + fallbackAlbums)\n            .asSequence()\n            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }\n',
    '        (baseAlbums + fallbackAlbums)\n            .asSequence()\n            .filter { it.releaseType == ReleaseType.Album }\n            .filter { isPlausibleYoutubeMusicAlbumTitle(it.title) && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }\n',
    'final album filter'
)
yt = replace_once(
    yt,
    '        val artist = tokens.firstOrNull { token -> !isAlbumLabel(token) && !token.matches(Regex("\\\\b(?:19|20)\\\\d{2}\\\\b")) }.orEmpty()\n',
    '''        val releaseType = tokens.firstNotNullOfOrNull { token ->
            levyraReleaseType(token).takeUnless { it == ReleaseType.Unknown }
        } ?: ReleaseType.Unknown
        val artist = tokens.firstOrNull { token ->
            levyraReleaseType(token) == ReleaseType.Unknown && !token.matches(Regex("\\b(?:19|20)\\d{2}\\b"))
        }.orEmpty()
''',
    'explore release parsing'
)
yt = replace_once(
    yt,
    '            explicit = card.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT")\n        )\n',
    '            explicit = card.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT"),\n            releaseType = releaseType\n        )\n',
    'explore release type'
)
yt = replace_once(
    yt,
    '                val album = parseCarouselAlbumHit(item) ?: continue\n                val key = "${album.title.lowercase()}|${album.artist.lowercase()}"\n',
    '                val album = parseCarouselAlbumHit(item) ?: continue\n                if (album.releaseType != ReleaseType.Album) continue\n                val key = "${album.title.lowercase()}|${album.artist.lowercase()}"\n',
    'home full album filter'
)
yt = replace_once(
    yt,
    '        val kind = tokens.firstOrNull().orEmpty()\n        if (!isAlbumLabel(kind)) return null\n',
    '        val kind = tokens.firstOrNull().orEmpty()\n        val releaseType = levyraReleaseType(kind)\n        if (releaseType == ReleaseType.Unknown) return null\n',
    'two row release classifier'
)
yt = replace_once(
    yt,
    '            artistBrowseId = artistReference?.browseId.orEmpty()\n        )\n    }\n\n    private fun parseAlbumHit',
    '            artistBrowseId = artistReference?.browseId.orEmpty(),\n            releaseType = releaseType\n        )\n    }\n\n    private fun parseAlbumHit',
    'two row release type'
)
yt = replace_once(
    yt,
    '        val kind = tokens.firstOrNull().orEmpty()\n        if (!isAlbumLabel(kind)) return null\n        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null\n',
    '        val kind = tokens.firstOrNull().orEmpty()\n        val releaseType = levyraReleaseType(kind)\n        if (releaseType == ReleaseType.Unknown) return null\n        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null\n',
    'responsive release classifier'
)
yt = replace_once(
    yt,
    '            artistBrowseId = artistReference?.browseId.orEmpty()\n        )\n    }\n\n    private fun isAlbumLabel',
    '            artistBrowseId = artistReference?.browseId.orEmpty(),\n            releaseType = releaseType\n        )\n    }\n\n    private fun isAlbumLabel',
    'responsive release type'
)
write(yt_path, yt)

# ---------------------------------------------------------------------------
# Artist profile release separation
# ---------------------------------------------------------------------------
artist_path = 'app/src/main/java/com/luc4n3x/levyra/data/ArtistRepository.kt'
artist = read(artist_path)
artist = replace_once(
    artist,
    'import com.luc4n3x.levyra.domain.Track\n',
    'import com.luc4n3x.levyra.domain.Track\nimport com.luc4n3x.levyra.domain.ReleaseType\nimport com.luc4n3x.levyra.domain.isSingleLike\nimport com.luc4n3x.levyra.domain.releaseTypeFromProviderLabel\n',
    'artist release imports'
)
artist = replace_once(
    artist,
    '        val albums = mergeReleases(extractReleases(root, "Album"), expanded.albums)\n        val singles = mergeReleases(extractReleases(root, "Singol"), expanded.singles)\n',
    '''        val mergedAlbums = mergeReleases(extractReleases(root, "Album"), expanded.albums)
        val mergedSingles = mergeReleases(extractReleases(root, "Singol"), expanded.singles)
        val albums = mergedAlbums.filter { it.releaseType == ReleaseType.Album }
        val singles = (mergedSingles + mergedAlbums).filter { it.releaseType.isSingleLike }
            .distinctBy { it.browseId.ifBlank { "${it.title.lowercase()}|${it.year}" } }
        val compilations = (mergedAlbums + mergedSingles).filter { it.releaseType == ReleaseType.Compilation }
            .distinctBy { it.browseId.ifBlank { "${it.title.lowercase()}|${it.year}" } }
''',
    'artist release buckets'
)
artist = replace_once(
    artist,
    '            videosParams = videoPointer?.params.orEmpty()\n        )\n',
    '            videosParams = videoPointer?.params.orEmpty(),\n            compilations = compilations\n        )\n',
    'artist profile compilations'
)
artist = replace_once(
    artist,
    '            val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty().trim()\n            if (kindHint.isNotBlank() && !releaseKindMatches(subtitle, kindHint)) return@forEach\n',
    '''            val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            val parsedReleaseType = releaseTypeFromProviderLabel(subtitle)
            val releaseType = when {
                parsedReleaseType != ReleaseType.Unknown -> parsedReleaseType
                kindHint.startsWith("Singol", ignoreCase = true) -> ReleaseType.Single
                kindHint.startsWith("Album", ignoreCase = true) -> ReleaseType.Album
                else -> ReleaseType.Unknown
            }
            if (kindHint.startsWith("Album", ignoreCase = true) && releaseType != ReleaseType.Album && releaseType != ReleaseType.Compilation) return@forEach
            if (kindHint.startsWith("Singol", ignoreCase = true) && !releaseType.isSingleLike) return@forEach
''',
    'artist release classification'
)
artist = replace_once(
    artist,
    '                    explicit = card.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT")\n                )\n',
    '                    explicit = card.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT"),\n                    releaseType = releaseType\n                )\n',
    'artist release type'
)
write(artist_path, artist)

# ---------------------------------------------------------------------------
# ISRC-first matching in player and artwork resolver
# ---------------------------------------------------------------------------
write(
    'app/src/main/java/com/luc4n3x/levyra/data/RecordingIdentity.kt',
    r'''package com.luc4n3x.levyra.data

internal enum class RecordingIdentityMatch {
    Exact,
    Conflict,
    Unknown
}

internal fun normalizedIsrc(value: String): String = value
    .uppercase()
    .filter(Char::isLetterOrDigit)
    .takeIf { it.matches(Regex("[A-Z]{2}[A-Z0-9]{3}[0-9]{7}")) }
    .orEmpty()

internal fun recordingIdentityMatch(reference: String, candidate: String): RecordingIdentityMatch {
    val expected = normalizedIsrc(reference)
    val actual = normalizedIsrc(candidate)
    if (expected.isBlank() || actual.isBlank()) return RecordingIdentityMatch.Unknown
    return if (expected == actual) RecordingIdentityMatch.Exact else RecordingIdentityMatch.Conflict
}
'''
)

vm_path = 'app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt'
vm = read(vm_path)
vm = replace_once(
    vm,
    'import com.luc4n3x.levyra.data.isPlausibleYoutubeMusicAlbumTitle\n',
    'import com.luc4n3x.levyra.data.isPlausibleYoutubeMusicAlbumTitle\nimport com.luc4n3x.levyra.data.RecordingIdentityMatch\nimport com.luc4n3x.levyra.data.recordingIdentityMatch\n',
    'viewmodel recording imports'
)
vm = replace_once(
    vm,
    'internal fun isPlaybackCandidateCompatible(target: Track, candidate: Track): Boolean {\n    val targetTitle = playbackTextKey(target.title)\n',
    '''internal fun isPlaybackCandidateCompatible(target: Track, candidate: Track): Boolean {
    when (recordingIdentityMatch(target.isrc, candidate.isrc)) {
        RecordingIdentityMatch.Exact -> return true
        RecordingIdentityMatch.Conflict -> return false
        RecordingIdentityMatch.Unknown -> Unit
    }
    val targetTitle = playbackTextKey(target.title)
''',
    'isrc compatibility'
)
vm = replace_once(
    vm,
    'internal fun playbackCandidateScore(target: Track, candidate: Track): Int {\n    val targetTitle = playbackTextKey(target.title)\n',
    '''internal fun playbackCandidateScore(target: Track, candidate: Track): Int {
    when (recordingIdentityMatch(target.isrc, candidate.isrc)) {
        RecordingIdentityMatch.Exact -> return 10_000
        RecordingIdentityMatch.Conflict -> return Int.MIN_VALUE
        RecordingIdentityMatch.Unknown -> Unit
    }
    val targetTitle = playbackTextKey(target.title)
''',
    'isrc candidate score'
)
vm = replace_once(
    vm,
    '    private val repository = YoutubeMusicRepository(application.applicationContext)\n',
    '    private val repository = YoutubeMusicRepository(application.applicationContext)\n',
    'repository anchor'
)
vm = replace_once(
    vm,
    '    fun closeSettings() {\n        _state.update { it.copy(showSettings = false) }\n    }\n',
    '''    fun closeSettings() {
        _state.update { it.copy(showSettings = false) }
    }

    fun importYoutubeMusicSession(rawValue: String): Boolean {
        val saved = repository.importYoutubeMusicSession(rawValue)
        _state.update { it.copy(youtubeMusicAuthenticated = repository.hasYoutubeMusicSession()) }
        return saved
    }

    fun clearYoutubeMusicSession() {
        repository.clearYoutubeMusicSession()
        _state.update { it.copy(youtubeMusicAuthenticated = false) }
    }
''',
    'viewmodel youtube auth actions'
)
# Seed state after construction through the first LevyraUiState creation.
vm = replace_once(
    vm,
    '    private val _state = MutableStateFlow(LevyraUiState())\n',
    '    private val _state = MutableStateFlow(LevyraUiState(youtubeMusicAuthenticated = repository.hasYoutubeMusicSession()))\n',
    'viewmodel initial auth state'
)
write(vm_path, vm)

ui_state_path = 'app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraUiState.kt'
ui_state = read(ui_state_path)
ui_state = replace_once(
    ui_state,
    '    val showSettings: Boolean = false,\n',
    '    val showSettings: Boolean = false,\n    val youtubeMusicAuthenticated: Boolean = false,\n',
    'ui auth state'
)
write(ui_state_path, ui_state)

artwork_path = 'app/src/main/java/com/luc4n3x/levyra/data/OfficialArtworkRepository.kt'
artwork = read(artwork_path)
artwork = replace_once(
    artwork,
    '        val referenceIsrc = normalizeIdentifier(track.isrc)\n        val candidateIsrc = normalizeIdentifier(candidate.isrc)\n',
    '''        val referenceIsrc = normalizeIdentifier(track.isrc)
        val candidateIsrc = normalizeIdentifier(candidate.isrc)
        when (recordingIdentityMatch(referenceIsrc, candidateIsrc)) {
            RecordingIdentityMatch.Exact -> return 10_000
            RecordingIdentityMatch.Conflict -> return Int.MIN_VALUE
            RecordingIdentityMatch.Unknown -> Unit
        }
''',
    'artwork isrc gate'
)
write(artwork_path, artwork)

# ---------------------------------------------------------------------------
# Settings UI for importing the optional cookie
# ---------------------------------------------------------------------------
app_path = 'app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt'
app = read(app_path)
app = replace_once(
    app,
    'import androidx.compose.ui.text.style.TextOverflow\n',
    'import androidx.compose.ui.text.style.TextOverflow\nimport androidx.compose.ui.text.input.PasswordVisualTransformation\n',
    'password import'
)
app = replace_once(
    app,
    '                    playbackDiagnostics = state.playbackDiagnostics,\n',
    '                    playbackDiagnostics = state.playbackDiagnostics,\n                    youtubeMusicAuthenticated = state.youtubeMusicAuthenticated,\n',
    'settings auth state arg'
)
app = replace_once(
    app,
    '                    onShareDiagnostics = {\n',
    '                    onImportYoutubeMusicSession = viewModel::importYoutubeMusicSession,\n                    onClearYoutubeMusicSession = viewModel::clearYoutubeMusicSession,\n                    onShareDiagnostics = {\n',
    'settings auth callbacks'
)
app = replace_once(
    app,
    '    playbackDiagnostics: String,\n',
    '    playbackDiagnostics: String,\n    youtubeMusicAuthenticated: Boolean,\n',
    'settings auth parameter'
)
app = replace_once(
    app,
    '    onShareDiagnostics: () -> Unit,\n',
    '    onImportYoutubeMusicSession: (String) -> Boolean,\n    onClearYoutubeMusicSession: () -> Unit,\n    onShareDiagnostics: () -> Unit,\n',
    'settings auth callback params'
)
app = replace_once(
    app,
    '    var activeCategory by rememberSaveable { mutableStateOf<String?>(null) }\n',
    '''    var activeCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var youtubeMusicCookie by rememberSaveable { mutableStateOf("") }
    var youtubeMusicCookieError by rememberSaveable { mutableStateOf(false) }
''',
    'settings auth local state'
)
app = replace_once(
    app,
    '        SettingsCategoryMeta("backup", categoryTitle(strings.backupRestoreSection), "${strings.createDataBackup} · ${strings.restoreBackup}", Icons.Rounded.History, LevyraCyan),\n',
    '        SettingsCategoryMeta("accounts", categoryTitle(if (strings.code.startsWith("it")) "Account musicali" else "Music accounts"), if (youtubeMusicAuthenticated) "YouTube Music · connesso" else "YouTube Music · cookie facoltativo", Icons.Rounded.Person, LevyraCyan),\n        SettingsCategoryMeta("backup", categoryTitle(strings.backupRestoreSection), "${strings.createDataBackup} · ${strings.restoreBackup}", Icons.Rounded.History, LevyraCyan),\n',
    'accounts category'
)
accounts_case = r'''                        "accounts" -> {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    SettingsSectionLabel(if (strings.code.startsWith("it")) "YouTube Music" else "YouTube Music")
                                    Text(
                                        if (strings.code.startsWith("it")) {
                                            "Facoltativo. Incolla il cookie esportato da music.youtube.com: viene cifrato sul dispositivo, non entra nei backup e non viene inviato agli stream."
                                        } else {
                                            "Optional. Paste a cookie exported from music.youtube.com. It is encrypted on this device, excluded from backups and never attached to media streams."
                                        },
                                        color = LevyraMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color.White.copy(alpha = 0.05f),
                                        border = BorderStroke(1.dp, if (youtubeMusicAuthenticated) LevyraCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                if (youtubeMusicAuthenticated) {
                                                    if (strings.code.startsWith("it")) "Sessione salvata e protetta" else "Session saved and protected"
                                                } else {
                                                    if (strings.code.startsWith("it")) "Nessuna sessione salvata" else "No saved session"
                                                },
                                                color = if (youtubeMusicAuthenticated) LevyraCyan else LevyraText,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            OutlinedTextField(
                                                value = youtubeMusicCookie,
                                                onValueChange = {
                                                    youtubeMusicCookie = it
                                                    youtubeMusicCookieError = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                label = { Text(if (strings.code.startsWith("it")) "Cookie o export Netscape/JSON" else "Cookie or Netscape/JSON export") },
                                                visualTransformation = PasswordVisualTransformation(),
                                                minLines = 2,
                                                maxLines = 4,
                                                isError = youtubeMusicCookieError,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = LevyraText,
                                                    unfocusedTextColor = LevyraText,
                                                    focusedBorderColor = LevyraCyan,
                                                    unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                                                    cursorColor = LevyraCyan
                                                )
                                            )
                                            if (youtubeMusicCookieError) {
                                                Text(
                                                    if (strings.code.startsWith("it")) "Cookie non valido: deve contenere SAPISID o __Secure-3PAPISID." else "Invalid cookie: SAPISID or __Secure-3PAPISID is required.",
                                                    color = LevyraPink,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(
                                                    onClick = {
                                                        val saved = onImportYoutubeMusicSession(youtubeMusicCookie)
                                                        youtubeMusicCookieError = !saved
                                                        if (saved) youtubeMusicCookie = ""
                                                    },
                                                    enabled = youtubeMusicCookie.isNotBlank()
                                                ) {
                                                    Text(if (strings.code.startsWith("it")) "Salva sessione" else "Save session")
                                                }
                                                if (youtubeMusicAuthenticated) {
                                                    TextButton(onClick = {
                                                        onClearYoutubeMusicSession()
                                                        youtubeMusicCookie = ""
                                                        youtubeMusicCookieError = false
                                                    }) {
                                                        Text(if (strings.code.startsWith("it")) "Disconnetti" else "Disconnect", color = LevyraPink)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
'''
app = replace_once(
    app,
    '                        "backup" -> {\n',
    accounts_case + '                        "backup" -> {\n',
    'accounts settings body'
)
write(app_path, app)

# ---------------------------------------------------------------------------
# Cache persistence for release type
# ---------------------------------------------------------------------------
prefs_path = 'app/src/main/java/com/luc4n3x/levyra/data/LevyraPreferences.kt'
prefs = read(prefs_path)
prefs = replace_once(
    prefs,
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\n',
    'import com.luc4n3x.levyra.domain.LevyraInterfaceSettings\nimport com.luc4n3x.levyra.domain.ReleaseType\n',
    'preference release import'
)
prefs = replace_once(
    prefs,
    '                    .put("metadataConfidence", album.metadataConfidence)\n',
    '                    .put("metadataConfidence", album.metadataConfidence)\n                    .put("releaseType", album.releaseType.name)\n',
    'serialize release type'
)
prefs = replace_once(
    prefs,
    '                        metadataConfidence = item.optInt("metadataConfidence").coerceIn(0, 100)\n',
    '                        metadataConfidence = item.optInt("metadataConfidence").coerceIn(0, 100),\n                        releaseType = runCatching { ReleaseType.valueOf(item.optString("releaseType")) }.getOrDefault(ReleaseType.Unknown)\n',
    'deserialize release type'
)
write(prefs_path, prefs)

# ---------------------------------------------------------------------------
# Spotify collector: batch track metadata for ISRC and album type
# ---------------------------------------------------------------------------
spotify_path = 'tools/levyra-editorial/levyra_editorial/spotify.py'
spotify = read(spotify_path)
spotify = replace_once(
    spotify,
    '        return output\n\n    def close(self) -> None:\n',
    '''        return self._enrich_track_metadata(output)

    def _enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:
        if self._access_token is None:
            self.authenticate()
        ids = [
            str(item.get("track", {}).get("id") or "").strip()
            for item in items
            if isinstance(item.get("track"), Mapping)
        ]
        metadata: dict[str, Mapping[str, Any]] = {}
        for offset in range(0, len(ids), 50):
            chunk = [value for value in ids[offset : offset + 50] if value]
            if not chunk:
                continue
            response = self._session.get(
                f"{API_BASE_URL}/tracks",
                params={"ids": ",".join(chunk)},
                headers=self._api_headers(),
                timeout=self._timeout,
            )
            if response.status_code == 401:
                self.authenticate()
                response = self._session.get(
                    f"{API_BASE_URL}/tracks",
                    params={"ids": ",".join(chunk)},
                    headers=self._api_headers(),
                    timeout=self._timeout,
                )
            if response.status_code >= 400:
                LOGGER.warning("Spotify track metadata enrichment failed with HTTP %s.", response.status_code)
                continue
            payload = response.json()
            raw_tracks = payload.get("tracks") if isinstance(payload, Mapping) else None
            if not isinstance(raw_tracks, list):
                continue
            for raw_track in raw_tracks:
                if isinstance(raw_track, Mapping):
                    track_id = _string(raw_track.get("id"))
                    if track_id:
                        metadata[track_id] = raw_track
        for item in items:
            track = item.get("track")
            if not isinstance(track, dict):
                continue
            enriched = metadata.get(str(track.get("id") or ""))
            if not isinstance(enriched, Mapping):
                continue
            external_ids = enriched.get("external_ids")
            if isinstance(external_ids, Mapping):
                track["external_ids"] = dict(external_ids)
            for key in ("track_number", "disc_number"):
                if isinstance(enriched.get(key), int):
                    track[key] = enriched[key]
            album = track.get("album")
            enriched_album = enriched.get("album")
            if isinstance(album, dict) and isinstance(enriched_album, Mapping):
                for key in ("album_type", "total_tracks", "release_date"):
                    value = enriched_album.get(key)
                    if value is not None:
                        album[key] = value
        return items

    def _api_headers(self) -> dict[str, str]:
        if self._access_token is None:
            raise AuthenticationError("The editorial source is not authenticated.")
        headers = {"Authorization": f"Bearer {self._access_token}", "Accept": "application/json"}
        if self._client_id:
            headers["Client-Id"] = self._client_id
        return headers

    def close(self) -> None:
''',
    'spotify metadata enrichment'
)
write(spotify_path, spotify)

collector_path = 'tools/levyra-editorial/levyra_editorial/collector.py'
collector = read(collector_path)
collector = replace_once(
    collector,
    '            external_url=_nested_string(raw_album, "external_urls", "spotify"),\n        )\n',
    '            external_url=_nested_string(raw_album, "external_urls", "spotify"),\n            album_type=_optional_string(raw_album.get("album_type")),\n            total_tracks=_positive_int(raw_album.get("total_tracks")),\n        )\n',
    'collector album metadata'
)
collector = replace_once(
    collector,
    '                artwork_url=artwork_url,\n            )\n',
    '                artwork_url=artwork_url,\n                isrc=_nested_string(raw_track, "external_ids", "isrc"),\n            )\n',
    'collector track isrc'
)
write(collector_path, collector)

python_models_path = 'tools/levyra-editorial/levyra_editorial/models.py'
python_models = read(python_models_path)
python_models = replace_once(
    python_models,
    '    external_url: str | None\n\n\n@dataclass(frozen=True)\nclass Track:',
    '    external_url: str | None\n    album_type: str | None = None\n    total_tracks: int | None = None\n\n\n@dataclass(frozen=True)\nclass Track:',
    'python album release fields'
)
python_models = replace_once(
    python_models,
    '    artwork_url: str | None\n\n    def to_dict',
    '    artwork_url: str | None\n    isrc: str | None = None\n\n    def to_dict',
    'python track isrc field'
)
python_models = replace_once(
    python_models,
    '                    "releaseDate": self.album.release_date,\n',
    '                    "releaseDate": self.album.release_date,\n                    "type": self.album.album_type,\n                    "totalTracks": self.album.total_tracks,\n',
    'public album release fields'
)
python_models = replace_once(
    python_models,
    '                "explicit": self.explicit,\n',
    '                "explicit": self.explicit,\n                "isrc": self.isrc,\n',
    'public isrc'
)
python_models = python_models.replace('source (page URLs, ids, ISRC, credentials) stays out.', 'source (page URLs, ids and credentials) stays out; ISRC is a public recording identity.')
write(python_models_path, python_models)

workflow_path = '.github/workflows/editorial-catalog.yml'
workflow = read(workflow_path)
workflow = workflow.replace('has("isrc") or has("uri")', 'has("uri")')
workflow = workflow.replace('source identifiers or unsupported ISRC fields', 'source identifiers')
validation_anchor = '''          # Cover artwork is published on purpose, but only from the source image CDN.
'''
isrc_validation = '''          if jq -e 'any(.. | objects | select(has("isrc")); (.isrc | test("^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$") | not))' build/editorial/catalog.json >/dev/null; then
            echo "::error::Catalog contains a malformed ISRC"
            exit 1
          fi
'''
if workflow.count(validation_anchor) != 2:
    raise SystemExit('editorial ISRC guard: expected two anchors')
workflow = workflow.replace(validation_anchor, isrc_validation + validation_anchor)
write(workflow_path, workflow)

editorial_path = 'app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt'
editorial = read(editorial_path)
editorial = editorial.replace('source page URLs, URIs or ISRC.', 'source page URLs or URIs. ISRC is retained as a public recording identity.')
editorial = replace_once(
    editorial,
    '                explicit = item.optBoolean("explicit", false),\n',
    '                explicit = item.optBoolean("explicit", false),\n                isrc = item.optString("isrc").uppercase(Locale.ROOT).filter(Char::isLetterOrDigit),\n',
    'editorial isrc parsing'
)
write(editorial_path, editorial)

# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------
write(
    'app/src/test/java/com/luc4n3x/levyra/data/RecordingIdentityTest.kt',
    r'''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.isPlaybackCandidateCompatible
import com.luc4n3x.levyra.viewmodel.playbackCandidateScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingIdentityTest {
    @Test
    fun exactIsrcWinsBeforeTextMatching() {
        val target = Track(id = "spotify", title = "Completely different", artist = "A", album = "X", durationMs = 1, streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Spotify", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0, isrc = "IT-B00-20-00001")
        val candidate = target.copy(id = "youtube", title = "Other upload", isrc = "ITB002000001")
        assertTrue(isPlaybackCandidateCompatible(target, candidate))
        assertEquals(10_000, playbackCandidateScore(target, candidate))
    }

    @Test
    fun conflictingIsrcRejectsAnOtherwisePerfectCandidate() {
        val target = Track(id = "spotify", title = "Song", artist = "Artist", album = "Album", durationMs = 180000, streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "Spotify", moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0, isrc = "ITB002000001")
        val candidate = target.copy(id = "youtube", isrc = "USAAA2100001")
        assertFalse(isPlaybackCandidateCompatible(target, candidate))
        assertEquals(Int.MIN_VALUE, playbackCandidateScore(target, candidate))
    }
}
'''
)
write(
    'app/src/test/java/com/luc4n3x/levyra/domain/ReleaseTypeTest.kt',
    r'''package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseTypeTest {
    @Test
    fun separatesAlbumsSinglesEpsAndCompilations() {
        assertEquals(ReleaseType.Album, releaseTypeFromProviderLabel("Album"))
        assertEquals(ReleaseType.Single, releaseTypeFromProviderLabel("Singolo"))
        assertEquals(ReleaseType.Ep, releaseTypeFromProviderLabel("EP"))
        assertEquals(ReleaseType.Compilation, releaseTypeFromProviderLabel("Compilation"))
        assertEquals(ReleaseType.Compilation, releaseTypeFromProviderLabel("Raccolta"))
        assertEquals(ReleaseType.Unknown, releaseTypeFromProviderLabel("23 Mln riproduzioni"))
    }
}
'''
)
write(
    'app/src/test/java/com/luc4n3x/levyra/data/security/YoutubeMusicCookieParserTest.kt',
    r'''package com.luc4n3x.levyra.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicCookieParserTest {
    @Test
    fun acceptsHeaderJsonAndNetscapeExportsWithoutKeepingUnknownCookies() {
        val header = YoutubeMusicCookieParser.parse("SAPISID=abc123; SID=sid123; EVIL=secret")
        assertNotNull(header)
        assertTrue(header!!.cookieHeader.contains("SAPISID=abc123"))
        assertFalse(header.cookieHeader.contains("EVIL"))

        val json = YoutubeMusicCookieParser.parse("{\"__Secure-3PAPISID\":\"secure123\",\"PREF\":\"hl=it\"}")
        assertNotNull(json)

        val netscape = YoutubeMusicCookieParser.parse(".youtube.com\tTRUE\t/\tTRUE\t0\tSAPISID\tnetscape123")
        assertNotNull(netscape)
    }

    @Test
    fun rejectsExportsWithoutAnAuthenticationCookie() {
        assertNull(YoutubeMusicCookieParser.parse("PREF=hl=it; YSC=test"))
    }
}
'''
)

# Python collector tests for public ISRC/release metadata.
test_path = 'tools/levyra-editorial/tests/test_collector.py'
test = read(test_path)
if 'test_catalog_keeps_public_isrc_and_release_type' not in test:
    test += r'''


def test_catalog_keeps_public_isrc_and_release_type() -> None:
    item = _playlist_item(1)
    item["track"]["external_ids"] = {"isrc": "ITB002000001"}
    item["track"]["album"]["album_type"] = "album"
    item["track"]["album"]["total_tracks"] = 12
    catalog = build_catalog(_config(), FakeClient(items=[item]), generated_at="2026-01-01T00:00:00Z")
    public = catalog.to_dict()["collections"][0]["tracks"][0]
    assert public["isrc"] == "ITB002000001"
    assert public["album"]["type"] == "album"
    assert public["album"]["totalTracks"] == 12
'''
write(test_path, test)
