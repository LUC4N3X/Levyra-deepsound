package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

internal object JioSaavnMediaToken {
    private const val TRANSFORMATION = "DES/ECB/PKCS5Padding"
    private const val MAX_TOKEN_CHARS = 512
    private val key = SecretKeySpec("38346591".toByteArray(Charsets.US_ASCII), "DES")

    fun decode(token: String): String? {
        val trimmed = token.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_TOKEN_CHARS) return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)
            String(cipher.doFinal(Base64.getDecoder().decode(trimmed)), Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.startsWith("https://") }
    }
}
