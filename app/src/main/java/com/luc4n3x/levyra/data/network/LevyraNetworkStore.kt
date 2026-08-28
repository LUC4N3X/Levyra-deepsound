package com.luc4n3x.levyra.data.network

import android.content.Context
import com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore
import com.luc4n3x.levyra.domain.LevyraDnsMode
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraProxyMode

class LevyraNetworkStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val credentials = AndroidKeystoreCredentialStore(appContext)

    fun settings(): LevyraNetworkSettings = LevyraNetworkSettings(
        dnsMode = LevyraDnsMode.fromId(preferences.getString(KEY_DNS_MODE, null)),
        customDohUrl = preferences.getString(KEY_CUSTOM_DOH_URL, null).orEmpty(),
        proxyMode = LevyraProxyMode.fromId(preferences.getString(KEY_PROXY_MODE, null)),
        proxyHost = preferences.getString(KEY_PROXY_HOST, null).orEmpty(),
        proxyPort = preferences.getInt(KEY_PROXY_PORT, LevyraNetworkSettings.DEFAULT_PROXY_PORT),
        proxyUsername = preferences.getString(KEY_PROXY_USERNAME, null).orEmpty(),
        proxyAuthenticationEnabled = preferences.getBoolean(KEY_PROXY_AUTH, false),
        bypassProxyForStreams = preferences.getBoolean(KEY_STREAM_BYPASS, true)
    ).normalized()

    fun proxyPassword(): String =
        if (preferences.getBoolean(KEY_PROXY_AUTH, false)) {
            credentials.read(PROXY_PASSWORD_SLOT).orEmpty()
        } else {
            ""
        }

    fun hasProxyPassword(): Boolean = proxyPassword().isNotEmpty()

    fun save(settings: LevyraNetworkSettings) {
        val normalized = settings.normalized()
        preferences.edit()
            .putString(KEY_DNS_MODE, normalized.dnsMode.id)
            .putString(KEY_CUSTOM_DOH_URL, normalized.customDohUrl)
            .putString(KEY_PROXY_MODE, normalized.proxyMode.id)
            .putString(KEY_PROXY_HOST, normalized.proxyHost)
            .putInt(KEY_PROXY_PORT, normalized.proxyPort)
            .putString(KEY_PROXY_USERNAME, normalized.proxyUsername)
            .putBoolean(KEY_PROXY_AUTH, normalized.proxyAuthenticationEnabled)
            .putBoolean(KEY_STREAM_BYPASS, normalized.bypassProxyForStreams)
            .apply()
    }

    fun saveProxyPassword(value: String) {
        if (value.isEmpty()) {
            credentials.clear(PROXY_PASSWORD_SLOT)
        } else {
            credentials.write(PROXY_PASSWORD_SLOT, value)
        }
    }

    private companion object {
        const val PREFERENCES = "levyra_network"
        const val PROXY_PASSWORD_SLOT = "network_proxy_password"
        const val KEY_DNS_MODE = "dns_mode"
        const val KEY_CUSTOM_DOH_URL = "custom_doh_url"
        const val KEY_PROXY_MODE = "proxy_mode"
        const val KEY_PROXY_HOST = "proxy_host"
        const val KEY_PROXY_PORT = "proxy_port"
        const val KEY_PROXY_USERNAME = "proxy_username"
        const val KEY_PROXY_AUTH = "proxy_auth"
        const val KEY_STREAM_BYPASS = "stream_bypass"
    }
}

object LevyraNetworkController {
    fun applyStoredConfiguration(context: Context) {
        val store = LevyraNetworkStore(context)
        LevyraNetworkConfiguration.apply(store.settings(), store.proxyPassword())
    }

    fun apply(context: Context, settings: LevyraNetworkSettings, proxyPassword: String?) {
        val store = LevyraNetworkStore(context)
        store.save(settings)
        proxyPassword?.let(store::saveProxyPassword)
        LevyraNetworkConfiguration.apply(store.settings(), store.proxyPassword())
    }
}
