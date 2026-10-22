package com.luc4n3x.levyra.feature.radio

import java.util.UUID
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal enum class CustomRadioError { Name, StreamUrl, ImageUrl, Duplicate, Storage, Limit }

internal class CustomRadioException(val reason: CustomRadioError) : IllegalArgumentException(reason.name)

internal fun customRadioStation(name: String, url: String, image: String, existing: RadioStation? = null): RadioStation {
    val cleanName = name.trim()
    if (cleanName.length !in 1..160 || cleanName.any(Char::isISOControl)) throw CustomRadioException(CustomRadioError.Name)
    val stream = canonicalRadioUrl(url) ?: throw CustomRadioException(CustomRadioError.StreamUrl)
    val artwork = if (image.isBlank()) "" else canonicalRadioUrl(image) ?: throw CustomRadioException(CustomRadioError.ImageUrl)
    require(existing == null || existing.isCustom)
    return RadioStation(
        uuid = existing?.uuid ?: "custom:${UUID.randomUUID()}",
        name = cleanName,
        streamUrl = stream,
        resolvedStreamUrl = "",
        faviconUrl = artwork,
        homepageUrl = "",
        country = "",
        countryCode = "",
        language = "",
        tags = emptyList(),
        codec = "",
        bitrateKbps = 0,
        votes = 0,
        clickCount = 0,
        lastCheckOk = true,
        lastPlayedAt = existing?.lastPlayedAt ?: 0L
    )
}

internal fun canonicalRadioUrl(value: String): String? {
    val clean = value.trim()
    if (clean.length > 4096 || clean.any(Char::isISOControl) || !RadioUrlPolicy.isAllowed(clean)) return null
    return clean.toHttpUrlOrNull()?.newBuilder()?.fragment(null)?.build()?.toString()
}
