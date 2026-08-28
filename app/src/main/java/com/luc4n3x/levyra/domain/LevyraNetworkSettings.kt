package com.luc4n3x.levyra.domain

import java.net.URI

enum class LevyraDnsMode(val id: String) {
    System("system"),
    Cloudflare("cloudflare"),
    Google("google"),
    AdGuard("adguard"),
    Quad9("quad9"),
    Custom("custom");

    companion object {
        fun fromId(value: String?): LevyraDnsMode =
            entries.firstOrNull { it.id.equals(value?.trim(), ignoreCase = true) } ?: System
    }
}

enum class LevyraProxyMode(val id: String) {
    Disabled("disabled"),
    Http("http"),
    Socks("socks");

    companion object {
        fun fromId(value: String?): LevyraProxyMode =
            entries.firstOrNull { it.id.equals(value?.trim(), ignoreCase = true) } ?: Disabled
    }
}

data class LevyraNetworkSettings(
    val dnsMode: LevyraDnsMode = LevyraDnsMode.System,
    val customDohUrl: String = "",
    val proxyMode: LevyraProxyMode = LevyraProxyMode.Disabled,
    val proxyHost: String = "",
    val proxyPort: Int = DEFAULT_PROXY_PORT,
    val proxyUsername: String = "",
    val proxyAuthenticationEnabled: Boolean = false,
    val bypassProxyForStreams: Boolean = true
) {
    val usesProxy: Boolean get() = proxyMode != LevyraProxyMode.Disabled

    fun normalized(): LevyraNetworkSettings = copy(
        customDohUrl = customDohUrl.trim().take(MAX_URL_LENGTH),
        proxyHost = proxyHost.trim().take(MAX_HOST_LENGTH),
        proxyUsername = proxyUsername.trim().take(MAX_CREDENTIAL_LENGTH)
    )

    companion object {
        const val MIN_PORT = 1
        const val MAX_PORT = 65_535
        const val DEFAULT_PROXY_PORT = 8080
        const val MAX_HOST_LENGTH = 253
        const val MAX_URL_LENGTH = 512
        const val MAX_CREDENTIAL_LENGTH = 256
    }
}

enum class LevyraNetworkSettingsError {
    ProxyHostMissing,
    ProxyHostInvalid,
    ProxyPortOutOfRange,
    ProxyUsernameMissing,
    ProxyPasswordMissing,
    CustomDohUrlMissing,
    CustomDohUrlNotHttps,
    CustomDohUrlInvalid
}

object LevyraNetworkSettingsValidator {
    private val HOST_LABEL_PATTERN = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$")
    private val IPV4_PATTERN = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")

    fun validate(settings: LevyraNetworkSettings, hasProxyPassword: Boolean): List<LevyraNetworkSettingsError> {
        val errors = mutableListOf<LevyraNetworkSettingsError>()
        if (settings.dnsMode == LevyraDnsMode.Custom) {
            errors += validateCustomDohUrl(settings.customDohUrl)
        }
        if (settings.usesProxy) {
            val host = settings.proxyHost.trim()
            when {
                host.isBlank() -> errors += LevyraNetworkSettingsError.ProxyHostMissing
                host.length > LevyraNetworkSettings.MAX_HOST_LENGTH || !isValidProxyHost(host) ->
                    errors += LevyraNetworkSettingsError.ProxyHostInvalid
            }
            if (settings.proxyPort !in LevyraNetworkSettings.MIN_PORT..LevyraNetworkSettings.MAX_PORT) {
                errors += LevyraNetworkSettingsError.ProxyPortOutOfRange
            }
            if (settings.proxyAuthenticationEnabled) {
                val username = settings.proxyUsername.trim()
                if (username.isBlank() || username.length > LevyraNetworkSettings.MAX_CREDENTIAL_LENGTH) {
                    errors += LevyraNetworkSettingsError.ProxyUsernameMissing
                }
                if (!hasProxyPassword) errors += LevyraNetworkSettingsError.ProxyPasswordMissing
            }
        }
        return errors
    }

    fun validateCustomDohUrl(value: String): List<LevyraNetworkSettingsError> {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return listOf(LevyraNetworkSettingsError.CustomDohUrlMissing)
        if (trimmed.length > LevyraNetworkSettings.MAX_URL_LENGTH) {
            return listOf(LevyraNetworkSettingsError.CustomDohUrlInvalid)
        }
        if (!trimmed.startsWith("https://", ignoreCase = true)) {
            return listOf(LevyraNetworkSettingsError.CustomDohUrlNotHttps)
        }
        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return listOf(LevyraNetworkSettingsError.CustomDohUrlInvalid)
        val host = uri.host?.trim().orEmpty()
        if (host.isBlank() || uri.userInfo != null || uri.fragment != null) {
            return listOf(LevyraNetworkSettingsError.CustomDohUrlInvalid)
        }
        if (uri.port != -1 && uri.port !in LevyraNetworkSettings.MIN_PORT..LevyraNetworkSettings.MAX_PORT) {
            return listOf(LevyraNetworkSettingsError.CustomDohUrlInvalid)
        }
        return emptyList()
    }

    private fun isValidProxyHost(value: String): Boolean {
        if (IPV4_PATTERN.matches(value)) {
            return value.split('.').all { it.toIntOrNull() in 0..255 }
        }
        if (value.contains(':')) {
            val literal = value.removePrefix("[").removeSuffix("]")
            if (literal == value && (value.startsWith('[') || value.endsWith(']'))) return false
            if (!literal.contains(':') || literal.any {
                    !it.isDigit() && it !in 'a'..'f' && it !in 'A'..'F' && it != ':'
                }
            ) return false
            return runCatching { URI("http://[$literal]/").host != null }.getOrDefault(false)
        }
        if (value.length > LevyraNetworkSettings.MAX_HOST_LENGTH) return false
        return value.split('.').all { HOST_LABEL_PATTERN.matches(it) }
    }
}

enum class LevyraNetworkTestOutcome {
    Success,
    InvalidConfiguration,
    DnsResolutionFailed,
    ProxyAuthenticationFailed,
    Timeout,
    ConnectionRefused,
    TlsFailure,
    UnknownError
}
