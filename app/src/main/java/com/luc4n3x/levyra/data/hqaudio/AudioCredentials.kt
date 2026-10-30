package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.hqaudio.qobuz.QobuzCredentials
import com.luc4n3x.levyra.data.hqaudio.tidal.TidalCredentials
import org.json.JSONObject

internal class TidalCredentialSet(
    val refreshTokens: List<String>,
    val tvClientId: String,
    val tvClientSecret: String,
    val mobileClientId: String
) {
    fun sameAs(other: TidalCredentialSet): Boolean = refreshTokens == other.refreshTokens &&
        tvClientId == other.tvClientId && tvClientSecret == other.tvClientSecret && mobileClientId == other.mobileClientId

    fun clientId(hiRes: Boolean): String = if (hiRes) mobileClientId else tvClientId
}

internal class AudioCredentials(
    val revision: Long,
    val qobuz: List<QobuzCredentials.Account>,
    val tidal: TidalCredentialSet
) {
    companion object {
        const val MAX_DOCUMENT_BYTES = 65_536
        const val MAX_ACCOUNTS = 10

        fun embedded() = AudioCredentials(0, QobuzCredentials.accounts, TidalCredentialSet(
            listOf(TidalCredentials.refreshToken, TidalCredentials.fallbackRefreshToken).distinct(),
            TidalCredentials.tvClientId, TidalCredentials.tvClientSecret, TidalCredentials.mobileClientId
        ))

        fun parse(document: String): AudioCredentials? = runCatching {
            require(document.length <= MAX_DOCUMENT_BYTES && document.toByteArray(Charsets.UTF_8).size <= MAX_DOCUMENT_BYTES)
            val root = JSONObject(document)
            require(root.get("schemaVersion") == 1)
            val revision = root.get("revision")
            require(revision is Int || revision is Long)
            val version = (revision as Number).toLong()
            require(version > 0)
            val accounts = root.getJSONObject("qobuz").getJSONArray("accounts")
            require(accounts.length() in 1..MAX_ACCOUNTS)
            val qobuz = (0 until accounts.length()).map { index ->
                val account = accounts.getJSONObject(index)
                QobuzCredentials.Account(
                    account.credential("userAuthToken"), account.numericId("appId"),
                    account.credential("appSecret"), account.numericId("userId")
                )
            }
            require(qobuz.map { it.userAuthToken }.distinct().size == qobuz.size)
            val tidal = root.getJSONObject("tidal")
            val tokens = tidal.getJSONArray("refreshTokens")
            require(tokens.length() in 1..MAX_ACCOUNTS)
            val refreshTokens = (0 until tokens.length()).map { index ->
                (tokens.get(index) as String).also(::requireCredential)
            }
            require(refreshTokens.distinct().size == refreshTokens.size)
            AudioCredentials(version, qobuz, TidalCredentialSet(refreshTokens,
                tidal.credential("tvClientId"), tidal.credential("tvClientSecret"), tidal.credential("mobileClientId")))
        }.getOrNull()

        private fun JSONObject.credential(key: String): String = (get(key) as String).also(::requireCredential)

        private fun requireCredential(value: String) {
            require(value.length in 1..4096 && value.all { it.code in 33..126 })
        }

        private fun JSONObject.numericId(key: String): String = (get(key) as String).also { value ->
            require(value.length in 1..20 && value.all { it in '0'..'9' })
        }
    }
}

internal interface AudioCredentialsSource {
    suspend fun current(): AudioCredentials
    suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials
}
