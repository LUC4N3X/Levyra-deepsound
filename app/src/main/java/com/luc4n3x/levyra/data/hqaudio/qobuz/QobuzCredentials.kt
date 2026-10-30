package com.luc4n3x.levyra.data.hqaudio.qobuz

/**
 * Credential value type retained for isolated API tests.
 * No third-party Qobuz tokens or account secrets are distributed.
 */
internal object QobuzCredentials {
    class Account(
        val userAuthToken: String,
        val appId: String,
        val appSecret: String,
        val userId: String
    )
}
