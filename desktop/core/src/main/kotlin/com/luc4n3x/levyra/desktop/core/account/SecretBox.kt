package com.luc4n3x.levyra.desktop.core.account

import com.sun.jna.platform.win32.Crypt32Util
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class SecretBoxUnavailable : IOException("Secure storage is unavailable")

internal class SecretKeyUnavailable : IOException("Secure storage key is unavailable")

internal interface SecretBox {
    fun seal(plain: ByteArray): ByteArray
    fun open(sealed: ByteArray): ByteArray
}

internal class WindowsSecretBox : SecretBox {
    override fun seal(plain: ByteArray): ByteArray = try {
        Crypt32Util.cryptProtectData(plain, ENTROPY, 0, "Levyra", null)
    } catch (error: Exception) {
        throw SecretBoxUnavailable().apply { initCause(error) }
    }

    override fun open(sealed: ByteArray): ByteArray = try {
        Crypt32Util.cryptUnprotectData(sealed, ENTROPY, 0, null)
    } catch (error: Exception) {
        throw SecretBoxUnavailable().apply { initCause(error) }
    }

    private companion object {
        val ENTROPY = "levyra-ytm-session".toByteArray(Charsets.US_ASCII)
    }
}

internal class KeyedSecretBox(private val key: () -> ByteArray) : SecretBox {
    override fun seal(plain: ByteArray): ByteArray {
        val secret = key()
        try {
            val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secret, "AES"), GCMParameterSpec(128, nonce))
            return nonce + cipher.doFinal(plain)
        } finally {
            secret.fill(0)
        }
    }

    override fun open(sealed: ByteArray): ByteArray {
        if (sealed.size <= NONCE_BYTES) throw SecretBoxUnavailable()
        val secret = key()
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secret, "AES"), GCMParameterSpec(128, sealed, 0, NONCE_BYTES))
            return cipher.doFinal(sealed, NONCE_BYTES, sealed.size - NONCE_BYTES)
        } catch (error: java.security.GeneralSecurityException) {
            throw SecretBoxUnavailable().apply { initCause(error) }
        } finally {
            secret.fill(0)
        }
    }

    private companion object {
        const val NONCE_BYTES = 12
        val random = SecureRandom()
    }
}

internal object SecretKeys {
    private val random = SecureRandom()

    fun keyring(fallbackFile: Path): () -> ByteArray {
        val marker = fallbackFile.resolveSibling("${fallbackFile.fileName}.source")
        val lock = Any()
        var cached: ByteArray? = null
        return {
            synchronized(lock) {
                cached ?: resolveKey(fallbackFile, marker).also { cached = it }
            }.copyOf()
        }
    }

    private fun resolveKey(file: Path, marker: Path): ByteArray {
        val source = runCatching { Files.readString(marker).trim() }.getOrNull()
        if (source == FILE_SOURCE || (source == null && Files.isRegularFile(file))) return fileKey(file)
        val tool = secretToolPath()
        if (source == KEYRING_SOURCE) return tool?.let(::lookup) ?: throw SecretKeyUnavailable()
        tool?.let(::keyringKey)?.let { key ->
            writeMarker(marker, KEYRING_SOURCE)
            return key
        }
        return fileKey(file).also { writeMarker(marker, FILE_SOURCE) }
    }

    private fun keyringKey(tool: String): ByteArray? {
        lookup(tool)?.let { return it }
        val fresh = ByteArray(32).also(random::nextBytes)
        val encoded = Base64.getEncoder().encodeToString(fresh).toByteArray(Charsets.US_ASCII)
        runSecretTool(listOf(tool, "store", "--label=Levyra YouTube Music", *ATTRIBUTES), encoded) ?: return null
        return lookup(tool)?.takeIf { it.contentEquals(fresh) }
    }

    private fun lookup(tool: String): ByteArray? = runSecretTool(listOf(tool, "lookup", *ATTRIBUTES))?.let { output ->
        runCatching { Base64.getDecoder().decode(String(output, Charsets.US_ASCII).trim()) }.getOrNull()?.takeIf { it.size == 32 }
    }

    private fun writeMarker(marker: Path, source: String) {
        runCatching {
            Files.createDirectories(marker.parent)
            val temp = Files.createTempFile(marker.parent, "source", ".tmp")
            try {
                Files.writeString(temp, source)
                moveAtomically(temp, marker)
            } finally {
                Files.deleteIfExists(temp)
            }
        }
    }

    @Synchronized
    fun fileKey(file: Path): ByteArray {
        if (Files.isRegularFile(file)) {
            val existing = Files.readAllBytes(file)
            if (existing.size == 32) return existing
        }
        val fresh = ByteArray(32).also(random::nextBytes)
        Files.createDirectories(file.parent)
        val temp = Files.createTempFile(file.parent, "key", ".tmp")
        try {
            runCatching { Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------")) }
            Files.write(temp, fresh)
            moveAtomically(temp, file)
        } finally {
            Files.deleteIfExists(temp)
        }
        return fresh.copyOf()
    }

    private val ATTRIBUTES = arrayOf("application", "levyra", "purpose", "youtube-music-session")
    private const val KEYRING_SOURCE = "keyring"
    private const val FILE_SOURCE = "file"
}

internal fun secretToolPath(): String? = listOf("/usr/bin/secret-tool", "/usr/local/bin/secret-tool", "/bin/secret-tool")
    .firstOrNull { Files.isExecutable(Path.of(it)) }

internal fun runSecretTool(command: List<String>, input: ByteArray? = null, timeoutSeconds: Long = SECRET_TOOL_TIMEOUT_S): ByteArray? {
    val process = runCatching {
        ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    }.getOrNull() ?: return null
    return try {
        process.outputStream.use { stream -> input?.let(stream::write) }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS) || process.exitValue() != 0) null
        else process.inputStream.use { it.readBytes() }
    } catch (_: IOException) {
        null
    } catch (error: InterruptedException) {
        Thread.currentThread().interrupt()
        null
    } finally {
        if (process.isAlive) process.destroyForcibly()
    }
}

private const val SECRET_TOOL_TIMEOUT_S = 10L

internal fun moveAtomically(source: Path, target: Path) {
    try {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    }
}

internal fun platformSecretBox(root: Path): SecretBox =
    if (System.getProperty("os.name").orEmpty().startsWith("Windows")) WindowsSecretBox()
    else KeyedSecretBox(SecretKeys.keyring(root.resolve("youtube-music.key")))
