package com.luc4n3x.levyra.data.hqaudio.qobuz

import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal data class QobuzBackend(
    val id: String,
    val baseUrl: String,
    val sendsRegion: Boolean
) {
    private val base: HttpUrl = baseUrl.toHttpUrl()

    val host: String
        get() = base.host.lowercase(Locale.ROOT)

    fun searchUrl(query: String): String = base.newBuilder()
        .addPathSegments("api/get-music")
        .addQueryParameter("q", query)
        .addQueryParameter("offset", "0")
        .build()
        .toString()

    fun streamUrl(trackId: String, format: QobuzFormat): String = base.newBuilder()
        .addPathSegments("api/download-music")
        .addQueryParameter("track_id", trackId)
        .addQueryParameter("quality", format.code.toString())
        .build()
        .toString()
}

internal enum class QobuzFormat(val code: Int, val lossless: Boolean, val nominalKbps: Int) {
    HI_RES_192(27, true, 0),
    HI_RES_96(7, true, 0),
    CD(6, true, 0),
    MP3_320(5, false, 320);

    companion object {
        fun ladder(maxBitDepth: Int, maxSampleRateHz: Int, maximum: Boolean): List<QobuzFormat> {
            val ceiling = when {
                !maximum -> CD
                maxSampleRateHz > HI_RES_96_SAMPLE_RATE_HZ -> HI_RES_192
                maxBitDepth > CD_BIT_DEPTH || maxSampleRateHz > CD_MAX_SAMPLE_RATE_HZ -> HI_RES_96
                else -> CD
            }
            return entries.filter { it.ordinal >= ceiling.ordinal }
        }

        private const val CD_BIT_DEPTH = 16
        private const val CD_MAX_SAMPLE_RATE_HZ = 48_000
        private const val HI_RES_96_SAMPLE_RATE_HZ = 96_000
    }
}

internal object QobuzBackends {
    val defaults: List<QobuzBackend> = listOf(
        QobuzBackend(id = "kennyy", baseUrl = "https://qobuz.kennyy.com.br", sendsRegion = false),
        QobuzBackend(id = "trypt", baseUrl = "https://trypt-hifi-dl-456461932686.us-west1.run.app", sendsRegion = true)
    )

    val hosts: Set<String> = defaults.map { it.host }.toSet()

    private val supportedRegions = setOf(
        "US", "CA", "GB", "IE", "FR", "DE", "AT", "CH", "BE", "NL", "LU", "IT", "ES", "PT",
        "SE", "NO", "DK", "FI", "AU", "NZ", "JP", "BR", "MX", "AR", "CL", "CO"
    )

    fun region(locale: Locale = Locale.getDefault()): String =
        locale.country.uppercase(Locale.ROOT).takeIf { it in supportedRegions } ?: DEFAULT_REGION

    private const val DEFAULT_REGION = "US"
}
