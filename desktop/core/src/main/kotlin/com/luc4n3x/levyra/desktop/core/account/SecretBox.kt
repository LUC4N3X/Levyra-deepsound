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
        val key by lazy {
            (if (Files.isRegularFile(fallbackFile)) null else secretTool()?.let(::keyringKey)) ?: fileKey(fallbackFile)
        }
        return { key.copyOf() }
    }

    private fun keyringKey(tool: String): ByteArray? {
        lookup(tool)?.let { return it }
        val fresh = ByteArray(32).also(random::nextBytes)
        val encoded = Base64.getEncoder().encodeToString(fresh)
        val stored = runCatching {
            val process = ProcessBuilder(tool, "store", "--label=Levyra YouTube Music", *ATTRIBUTES)
                .redirectErrorStream(true).start()
            process.outputStream.use { it.write(encoded.toByteArray(Charsets.US_ASCII)) }
            process.inputStream.use { it.readBytes() }
            process.waitFor(TOOL_TIMEOUT_S, TimeUnit.SECONDS) && process.exitValue() == 0
        }.getOrDefault(false)
        return if (stored) lookup(tool)?.takeIf { it.contentEquals(fresh) } else null
    }

    private fun lookup(tool: String): ByteArray? = runCatching {
        val process = ProcessBuilder(tool, "lookup", *ATTRIBUTES).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        val output = process.inputStream.use { it.readBytes() }
        if (!process.waitFor(TOOL_TIMEOUT_S, TimeUnit.SECONDS) || process.exitValue() != 0) return null
        Base64.getDecoder().decode(String(output, Charsets.US_ASCII).trim()).takeIf { it.size == 32 }
    }.getOrNull()

    private fun secretTool(): String? = listOf("/usr/bin/secret-tool", "/usr/local/bin/secret-tool", "/bin/secret-tool")
        .firstOrNull { Files.isExecutable(Path.of(it)) }

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
    private const val TOOL_TIMEOUT_S = 10L
}

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
