package com.luc4n3x.levyra.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class YoutubeMusicCredential(
    val cookieHeader: String,
    val sapisid: String
) {
    fun authorizationHeader(nowMs: Long, origin: String = YOUTUBE_MUSIC_ORIGIN): String {
        val seconds = nowMs / 1_000L
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$seconds $sapisid $origin".toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "SAPISIDHASH ${seconds}_$digest"
    }
}

internal object YoutubeMusicCookieParser {
    fun parse(rawValue: String): YoutubeMusicCredential? {
        val raw = rawValue.trim()
        if (raw.isBlank() || raw.length > MAX_IMPORT_CHARS) return null
        val cookies = linkedMapOf<String, String>()
        parseJson(raw, cookies)
        parseNetscape(raw, cookies)
        parseHeader(raw, cookies)
        val safe = cookies
            .filterKeys(ALLOWED_COOKIE_NAMES::contains)
            .mapValues { (_, value) -> value.trim() }
            .filterValues { value -> value.isNotBlank() && value.length <= MAX_COOKIE_VALUE_CHARS && value.none(Char::isWhitespace) }
        val sapisid = sequenceOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")
            .mapNotNull(safe::get)
            .firstOrNull()
            ?: return null
        val header = safe.entries
            .sortedBy { it.key.lowercase(Locale.ROOT) }
            .joinToString("; ") { (name, value) -> "$name=$value" }
        return header.takeIf(String::isNotBlank)?.let { YoutubeMusicCredential(it, sapisid) }
    }

    private fun parseJson(raw: String, output: MutableMap<String, String>) {
        if (!raw.startsWith('{')) return
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return
        root.keys().forEach { key ->
            val value = root.optString(key).trim()
            if (key in ALLOWED_COOKIE_NAMES && value.isNotBlank()) output[key] = value
        }
    }

    private fun parseNetscape(raw: String, output: MutableMap<String, String>) {
        raw.lineSequence().forEach { line ->
            val clean = line.trim()
            if (clean.isBlank() || clean.startsWith('#')) return@forEach
            val columns = clean.split('\t')
            if (columns.size < 7) return@forEach
            val domain = columns[0].removePrefix("#HttpOnly_").lowercase(Locale.ROOT)
            if (domain != ".youtube.com" && domain != "youtube.com" && domain != "music.youtube.com") return@forEach
            val name = columns[5].trim()
            val value = columns[6].trim()
            if (name in ALLOWED_COOKIE_NAMES && value.isNotBlank()) output[name] = value
        }
    }

    private fun parseHeader(raw: String, output: MutableMap<String, String>) {
        val header = raw.substringAfter("Cookie:", raw).replace('\n', ';').replace('\r', ';')
        header.split(';').forEach { segment ->
            val index = segment.indexOf('=')
            if (index <= 0) return@forEach
            val name = segment.substring(0, index).trim()
            val value = segment.substring(index + 1).trim()
            if (name in ALLOWED_COOKIE_NAMES && value.isNotBlank()) output[name] = value
        }
    }

    private const val MAX_IMPORT_CHARS = 64 * 1024
    private const val MAX_COOKIE_VALUE_CHARS = 8 * 1024
    private val ALLOWED_COOKIE_NAMES = setOf(
        "SAPISID", "APISID", "SID", "HSID", "SSID", "LOGIN_INFO", "PREF", "YSC",
        "VISITOR_INFO1_LIVE", "VISITOR_PRIVACY_METADATA", "SOCS", "CONSENT",
        "__Secure-1PAPISID", "__Secure-3PAPISID", "__Secure-1PSID", "__Secure-3PSID",
        "__Secure-1PSIDTS", "__Secure-3PSIDTS", "__Secure-1PSIDCC", "__Secure-3PSIDCC"
    )
}

internal class YoutubeMusicCredentialStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))

    @Synchronized
    fun save(rawValue: String): Boolean {
        val credential = YoutubeMusicCookieParser.parse(rawValue) ?: return false
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(credential.cookieHeader.toByteArray(Charsets.UTF_8))
        val payload = JSONObject()
            .put("version", 1)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()
            .toByteArray(Charsets.UTF_8)
        val stream = runCatching { file.startWrite() }.getOrNull() ?: return false
        return try {
            stream.write(payload)
            stream.fd.sync()
            file.finishWrite(stream)
            true
        } catch (_: Throwable) {
            file.failWrite(stream)
            false
        }
    }

    @Synchronized
    fun load(): YoutubeMusicCredential? {
        val bytes = runCatching {
            file.openRead().use { input ->
                val buffer = ByteArray(MAX_FILE_BYTES + 1)
                var offset = 0
                while (offset < buffer.size) {
                    val count = input.read(buffer, offset, buffer.size - offset)
                    if (count < 0) break
                    offset += count
                }
                buffer.copyOf(offset)
            }
        }.getOrNull() ?: return null
        if (bytes.isEmpty() || bytes.size > MAX_FILE_BYTES) return null
        val root = runCatching { JSONObject(bytes.toString(Charsets.UTF_8)) }.getOrNull() ?: return null
        val iv = runCatching { Base64.decode(root.optString("iv"), Base64.NO_WRAP) }.getOrNull() ?: return null
        val encrypted = runCatching { Base64.decode(root.optString("data"), Base64.NO_WRAP) }.getOrNull() ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val decrypted = runCatching {
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull() ?: return null
        return YoutubeMusicCookieParser.parse(decrypted)
    }

    @Synchronized
    fun clear() {
        file.delete()
    }

    fun hasCredential(): Boolean = load() != null

    fun version(): Long = file.baseFile.lastModified()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE_NAME = "youtube_music_session.enc"
        const val KEY_ALIAS = "levyra.youtube.music.session.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val MAX_FILE_BYTES = 96 * 1024
    }
}

internal const val YOUTUBE_MUSIC_ORIGIN = "https://music.youtube.com"
