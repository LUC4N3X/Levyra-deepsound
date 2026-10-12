package com.luc4n3x.levyra.data.youtubemusic

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class YoutubeMusicSessionStore(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "youtube-music-session.enc"))

    fun load(): YoutubeMusicSavedSessions {
        if (!file.baseFile.exists()) return YoutubeMusicSavedSessions()
        val bytes = file.openRead().use { input ->
            if (file.baseFile.length() > MAX_BYTES) throw YoutubeMusicProtocolException()
            input.readBytes()
        }
        if (bytes.size < 29) throw YoutubeMusicProtocolException()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }
        return YoutubeMusicSessionCodec.decode(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8))
    }

    fun save(state: YoutubeMusicSavedSessions) {
        if (state.sessions.isEmpty()) {
            file.delete()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(YoutubeMusicSessionCodec.encode(state).toByteArray(Charsets.UTF_8))
        if (encrypted.size > MAX_BYTES) throw YoutubeMusicProtocolException()
        val output = file.startWrite()
        try {
            output.write(cipher.iv)
            output.write(encrypted)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    private fun key(): SecretKey = synchronized(KEY_LOCK) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        store.getKey(KEY_ALIAS, null) as? SecretKey ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    private companion object {
        val KEY_LOCK = Any()
        const val KEY_ALIAS = "levyra.youtube.music.v1"
        const val MAX_BYTES = 1024 * 1024
    }
}
