package com.luc4n3x.levyra.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeystoreCredentialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(name: String): String? {
        val stored = preferences.getString(name, null) ?: return null
        val parts = stored.split(':', limit = 2)
        if (parts.size != 2) return clearInvalid(name)
        return try {
            val plaintext = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP)))
            }.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
            plaintext.toString(Charsets.UTF_8).takeIf(String::isNotBlank) ?: clearInvalid(name)
        } catch (_: GeneralSecurityException) {
            clearInvalid(name)
        } catch (_: IllegalArgumentException) {
            clearInvalid(name)
        } catch (_: Throwable) {
            null
        }
    }

    fun write(name: String, value: String) {
        if (value.isBlank()) {
            clear(name)
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            preferences.edit().putString(
                name,
                "${Base64.encodeToString(cipher.iv, Base64.NO_WRAP)}:${Base64.encodeToString(encrypted, Base64.NO_WRAP)}"
            ).apply()
        } catch (_: Throwable) {
            clear(name)
        }
    }

    fun clear(vararg names: String) {
        preferences.edit().apply { names.forEach(::remove) }.apply()
    }

    private fun clearInvalid(name: String): Nothing? {
        clear(name)
        return null
    }

    private fun key(): SecretKey = synchronized(KEY_LOCK) {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        ).apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        val KEY_LOCK = Any()
        const val PREFERENCES = "levyra_external_credentials"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "levyra.external.credentials.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
