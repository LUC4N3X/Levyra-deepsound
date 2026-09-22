package com.luc4n3x.levyra.feature.radio

import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.util.Locale
import okhttp3.Dns

internal const val LIVE_RADIO_SOURCE = "Live Radio"

data class RadioStation(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val resolvedStreamUrl: String,
    val faviconUrl: String,
    val homepageUrl: String,
    val country: String,
    val countryCode: String,
    val language: String,
    val tags: List<String>,
    val codec: String,
    val bitrateKbps: Int,
    val votes: Int,
    val clickCount: Int,
    val lastCheckOk: Boolean,
    val lastPlayedAt: Long = 0L
) {
    val preferredStreamUrl: String
        get() = resolvedStreamUrl.takeIf(RadioUrlPolicy::isAllowed)
            ?: streamUrl.takeIf(RadioUrlPolicy::isAllowed)
            .orEmpty()

    val alternateStreamUrl: String
        get() = streamUrl.takeIf {
            it != preferredStreamUrl && RadioUrlPolicy.isAllowed(it)
        }.orEmpty()

    val safeFaviconUrl: String
        get() = faviconUrl.takeIf(RadioUrlPolicy::isAllowed).orEmpty()

    val qualityLabel: String
        get() = listOfNotNull(
            codec.trim().takeIf(String::isNotBlank),
            bitrateKbps.takeIf { it > 0 }?.let { "$it kbps" }
        ).joinToString(" / ")

    fun toTrack(stream: String = preferredStreamUrl): Track = Track(
        id = "live-radio:$uuid",
        title = name,
        artist = country.ifBlank { language }.ifBlank { LIVE_RADIO_SOURCE },
        album = qualityLabel,
        durationMs = 0L,
        streamUrl = stream,
        videoUrl = "",
        thumbnailUrl = faviconUrl.takeIf(RadioUrlPolicy::isAllowed).orEmpty(),
        largeThumbnailUrl = faviconUrl.takeIf(RadioUrlPolicy::isAllowed).orEmpty(),
        source = LIVE_RADIO_SOURCE,
        moodTags = tags.take(12).map { it.lowercase(Locale.ROOT) }.toSet(),
        energy = 50,
        vocal = 50,
        replayScore = votes.coerceIn(0, 100),
        cacheScore = clickCount.coerceIn(0, 100),
        accentStart = 0xFF00D7C7.toInt(),
        accentEnd = 0xFF087EA4.toInt()
    )
}

internal data class RadioDirectoryEntry(
    val name: String,
    val code: String = "",
    val stationCount: Int
)

internal data class RadioFilter(
    val category: RadioCategory = RadioCategory.Popular,
    val countryCode: String? = null,
    val language: String? = null,
    val offset: Int = 0,
    val limit: Int = 32
)

internal data class RadioLanguagePreference(
    val levyraCode: String,
    val radioLanguages: List<String>,
    val preferredCountries: List<String>
) {
    val primaryCountry: String get() = preferredCountries.first()
}

internal data class LiveRadioRetryPlan(val delayMs: Long, val streamUrl: String)

internal fun liveRadioRetryPlan(
    station: RadioStation,
    failedStreamUrl: String,
    attempt: Int
): LiveRadioRetryPlan? {
    val delayMs = LIVE_RADIO_RETRY_DELAYS_MS.getOrNull(attempt - 1) ?: return null
    val streamUrl = when {
        attempt == 1 && station.alternateStreamUrl.isNotBlank() &&
            failedStreamUrl != station.alternateStreamUrl -> station.alternateStreamUrl
        else -> station.preferredStreamUrl
    }
    return streamUrl.takeIf(String::isNotBlank)?.let { LiveRadioRetryPlan(delayMs, it) }
}

internal val LIVE_RADIO_RETRY_DELAYS_MS = longArrayOf(1_500L, 3_000L, 6_000L)

internal object RadioLanguagePreferences {
    private val values = listOf(
        preference("en", "English", "GB", "US", "CA", "AU", "IE", "NZ"),
        preference("it", "Italian", "IT", "CH", "SM"),
        preference("es", "Spanish", "ES", "MX", "AR", "CO", "CL", "PE"),
        preference("fr", "French", "FR", "BE", "CA", "CH", "SN"),
        preference("de", "German", "DE", "AT", "CH"),
        preference("pt", "Portuguese", "PT", "BR", "AO"),
        preference("nl", "Dutch", "NL", "BE", "SR"),
        preference("pl", "Polish", "PL"),
        preference("ro", "Romanian", "RO", "MD"),
        preference("el", "Greek", "GR", "CY"),
        preference("sv", "Swedish", "SE", "FI"),
        preference("da", "Danish", "DK", "GL"),
        preference("cs", "Czech", "CZ"),
        preference("sk", "Slovak", "SK"),
        preference("hr", "Croatian", "HR"),
        preference("bg", "Bulgarian", "BG"),
        preference("hu", "Hungarian", "HU"),
        preference("fi", "Finnish", "FI"),
        preference("et", "Estonian", "EE"),
        preference("nb", "Norwegian", "NO"),
        preference("ca", "Catalan", "ES", "AD"),
        preference("uk", "Ukrainian", "UA"),
        preference("ru", "Russian", "RU", "BY", "KZ"),
        preference("tr", "Turkish", "TR", "CY"),
        preference("ar", "Arabic", "SA", "EG", "AE", "MA", "DZ", "JO", "LB"),
        preference("fa", "Persian", "IR"),
        preference("zh", "Chinese", "CN", "TW", "HK", "SG"),
        preference("zh-Hant", "Chinese", "TW", "HK", "MO"),
        preference("ja", "Japanese", "JP"),
        preference("ko", "Korean", "KR"),
        preference("hi", "Hindi", "IN"),
        preference("id", "Indonesian", "ID"),
        preference("ms", "Malay", "MY", "BN", "SG"),
        preference("vi", "Vietnamese", "VN"),
        preference("th", "Thai", "TH"),
        preference("fil", "Filipino", "PH"),
        preference("he", "Hebrew", "IL")
    ).associateBy { it.levyraCode }

    init {
        check(values.keys == LevyraLanguageCatalog.languages.map { it.code }.toSet())
    }

    fun forLevyraLanguage(code: String): RadioLanguagePreference =
        values.getValue(LevyraLanguageCatalog.normalize(code))

    private fun preference(code: String, language: String, vararg countries: String) =
        RadioLanguagePreference(code, listOf(language), countries.toList())
}

internal object RadioUrlPolicy {
    private val privateIpv4Ranges = listOf(
        Regex("^10\\."),
        Regex("^127\\."),
        Regex("^169\\.254\\."),
        Regex("^192\\.168\\."),
        Regex("^172\\.(1[6-9]|2[0-9]|3[01])\\.")
    )

    val publicDns: Dns = publicDns { host ->
        InetAddress.getAllByName(host).toList()
    }

    internal fun publicDns(dnsLookup: (String) -> List<InetAddress>): Dns = Dns { hostname ->
        val addresses = try {
            dnsLookup(hostname)
        } catch (e: Exception) {
            if (e is UnknownHostException) throw e
            throw UnknownHostException("Failed to resolve radio host: $hostname").apply { initCause(e) }
        }
        if (addresses.isEmpty() || addresses.any { !SafeImageUrlPolicy.isPublicAddress(it) }) {
            throw UnknownHostException("Blocked non-public radio host: $hostname")
        }
        addresses
    }

    fun isAllowed(value: String): Boolean {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) return false
        if (!uri.userInfo.isNullOrBlank() || uri.host.isNullOrBlank()) return false
        val host = uri.host.lowercase(Locale.ROOT).removePrefix("[").removeSuffix("]")
        if (host == "localhost" || host.endsWith(".localhost") || host == "0.0.0.0" || host == "::1") return false
        if (privateIpv4Ranges.any { it.containsMatchIn(host) }) return false
        if (host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")) return false
        return true
    }
}

internal fun Track.isLiveRadio(): Boolean = source == LIVE_RADIO_SOURCE && id.startsWith("live-radio:")

private val radioNameSeparatorPattern = Regex("[^\\p{L}\\p{N}]+")

internal fun normalizeRadioName(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace(radioNameSeparatorPattern, " ")
    .trim()

internal fun radioSearchTokens(value: String): List<String> =
    normalizeRadioName(value)
        .split(' ')
        .map(String::trim)
        .filter { it.length >= 2 }
        .distinct()
        .take(8)

internal fun radioStationMatchesSearch(station: RadioStation, query: String): Boolean {
    val tokens = radioSearchTokens(query)
    if (tokens.isEmpty()) return false
    val searchable = normalizeRadioName(
        listOf(
            station.name,
            station.country,
            station.countryCode,
            station.language,
            station.tags.joinToString(" ")
        ).joinToString(" ")
    )
    return tokens.all { token -> searchable.contains(token) }
}


internal fun radioStationScore(station: RadioStation): Long {
    val codecScore = when (station.codec.uppercase(Locale.ROOT)) {
        "AAC", "AAC+", "MP3", "OGG", "OPUS", "FLAC" -> 4_000L
        else -> 0L
    }
    val bitrateScore = when (station.bitrateKbps) {
        in 48..320 -> 3_000L
        in 24..512 -> 1_000L
        else -> 0L
    }
    val httpsScore = if (station.preferredStreamUrl.startsWith("https://", true)) 2_000L else 0L
    return station.votes.toLong().coerceAtMost(200_000L) * 12L +
        station.clickCount.toLong().coerceAtMost(1_000_000L) + codecScore + bitrateScore + httpsScore
}

internal fun filterAndRankRadioStations(stations: List<RadioStation>): List<RadioStation> {
    val candidates = stations.asSequence()
        .filter { it.lastCheckOk }
        .filter { it.uuid.isNotBlank() && it.name.trim().length in 2..160 }
        .filter { it.preferredStreamUrl.isNotBlank() }
        .sortedByDescending(::radioStationScore)
        .toList()
    val seenUuids = hashSetOf<String>()
    val seenSignatures = hashSetOf<String>()
    return candidates.filter { station ->
        val uuid = station.uuid.lowercase(Locale.ROOT)
        val streamUri = runCatching { URI(station.preferredStreamUrl) }.getOrNull()
        val host = streamUri?.host.orEmpty().lowercase(Locale.ROOT)
        val streamPath = streamUri?.let { "${it.path.orEmpty()}?${it.query.orEmpty()}" }.orEmpty()
        val signature = "${normalizeRadioName(station.name)}|$host|$streamPath|${station.countryCode.uppercase(Locale.ROOT)}"
        seenUuids.add(uuid) && seenSignatures.add(signature)
    }
}

internal fun filterAndRankRadioSearchResults(
    stations: List<RadioStation>,
    query: String
): List<RadioStation> {
    val normalizedQuery = normalizeRadioName(query)
    val candidates = stations.asSequence()
        .filter { it.uuid.isNotBlank() && it.name.trim().length in 2..160 }
        .filter { it.preferredStreamUrl.isNotBlank() }
        .filter { radioStationMatchesSearch(it, query) }
        .sortedByDescending { station ->
            radioStationScore(station) +
                if (station.lastCheckOk) 5_000_000L else 0L +
                if (normalizeRadioName(station.name) == normalizedQuery) 2_000_000L else 0L
        }
        .toList()
    val seenUuids = hashSetOf<String>()
    val seenSignatures = hashSetOf<String>()
    return candidates.filter { station ->
        val uuid = station.uuid.lowercase(Locale.ROOT)
        val streamUri = runCatching { URI(station.preferredStreamUrl) }.getOrNull()
        val host = streamUri?.host.orEmpty().lowercase(Locale.ROOT)
        val streamPath = streamUri?.let { "${it.path.orEmpty()}?${it.query.orEmpty()}" }.orEmpty()
        val signature = "${normalizeRadioName(station.name)}|$host|$streamPath|${station.countryCode.uppercase(Locale.ROOT)}"
        seenUuids.add(uuid) && seenSignatures.add(signature)
    }
}
