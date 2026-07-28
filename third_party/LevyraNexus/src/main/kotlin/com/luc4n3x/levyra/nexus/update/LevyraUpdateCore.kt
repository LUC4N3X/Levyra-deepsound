package com.luc4n3x.levyra.nexus.update

import com.luc4n3x.levyra.nexus.core.LevyraDataSource
import com.luc4n3x.levyra.nexus.core.LevyraFailureType
import com.luc4n3x.levyra.nexus.core.LevyraFreshness
import com.luc4n3x.levyra.nexus.core.LevyraResult
import java.io.InputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.Locale


enum class LevyraUpdateChannel {
    STABLE,
    BETA,
    NIGHTLY
}

data class LevyraUpdateArtifact(
    val name: String,
    val downloadUrl: String,
    val mirrors: List<String> = emptyList(),
    val sizeBytes: Long = 0L,
    val contentType: String = "",
    val abi: String = "universal",
    val minSdk: Int = 0,
    val sha256: String = "",
    val signatureBase64: String = ""
) {
    val isApk: Boolean
        get() = name.endsWith(".apk", ignoreCase = true) || contentType.contains("android.package-archive", ignoreCase = true)
}

data class LevyraUpdateManifest(
    val channel: LevyraUpdateChannel,
    val versionCode: Long,
    val versionName: String,
    val packageName: String,
    val rolloutPercent: Int = 100,
    val minimumSupportedVersionCode: Long = 0L,
    val artifacts: List<LevyraUpdateArtifact>,
    val changelog: Map<String, List<String>> = emptyMap(),
    val signatureAlgorithm: String = "SHA256withECDSA",
    val signatureBase64: String = ""
) {
    init {
        require(versionCode >= 0L)
        require(rolloutPercent in 0..100)
    }

    fun canonicalPayload(): ByteArray {
        val values = buildList {
            add("channel" to channel.name)
            add("versionCode" to versionCode.toString())
            add("versionName" to versionName)
            add("packageName" to packageName)
            add("rolloutPercent" to rolloutPercent.toString())
            add("minimumSupportedVersionCode" to minimumSupportedVersionCode.toString())
            artifacts.sortedBy { it.name.lowercase(Locale.ROOT) }.forEachIndexed { index, artifact ->
                add("artifact.$index.name" to artifact.name)
                add("artifact.$index.downloadUrl" to artifact.downloadUrl)
                add("artifact.$index.mirrors" to artifact.mirrors.sorted().joinToString("\u001f"))
                add("artifact.$index.sizeBytes" to artifact.sizeBytes.toString())
                add("artifact.$index.contentType" to artifact.contentType)
                add("artifact.$index.abi" to artifact.abi)
                add("artifact.$index.minSdk" to artifact.minSdk.toString())
                add("artifact.$index.sha256" to artifact.sha256.lowercase(Locale.ROOT))
                add("artifact.$index.signatureBase64" to artifact.signatureBase64)
            }
            changelog.toSortedMap().forEach { (language, lines) ->
                add("changelog.$language" to lines.joinToString("\u001f"))
            }
        }
        return buildString {
            values.forEach { (name, value) ->
                append(name.length).append(':').append(name)
                append(value.length).append(':').append(value)
                append('\n')
            }
        }.toByteArray(Charsets.UTF_8)
    }
}

data class LevyraArtifactInspection(
    val packageName: String,
    val signerSha256: String,
    val sdk: Int,
    val abi: String
)

data class LevyraVerifiedArtifact(
    val artifact: LevyraUpdateArtifact,
    val inspection: LevyraArtifactInspection
)

object LevyraVersionComparator {
    fun compare(left: String, right: String): Int {
        val leftParts = numericParts(left)
        val rightParts = numericParts(right)
        val size = maxOf(leftParts.size, rightParts.size, 1)
        for (index in 0 until size) {
            val leftValue = leftParts.getOrElse(index) { 0 }
            val rightValue = rightParts.getOrElse(index) { 0 }
            if (leftValue != rightValue) return leftValue.compareTo(rightValue)
        }
        return qualifierRank(left).compareTo(qualifierRank(right))
    }

    private fun numericParts(value: String): List<Int> {
        val core = Regex("\\d+(?:\\.\\d+)*").find(value.trim())?.value.orEmpty()
        return core.split('.').mapNotNull(String::toIntOrNull)
    }

    private fun qualifierRank(value: String): Int {
        val normalized = value.lowercase(Locale.ROOT)
        return when {
            "nightly" in normalized || "snapshot" in normalized -> 0
            "alpha" in normalized -> 1
            "beta" in normalized -> 2
            "rc" in normalized -> 3
            else -> 4
        }
    }
}

object LevyraUpdateSelector {
    private val sidecarSuffixes = listOf(".sha256", ".sha512", ".sig", ".asc", ".json")

    fun selectArtifact(
        artifacts: List<LevyraUpdateArtifact>,
        sdk: Int,
        supportedAbis: List<String>,
        preferredHost: String = ""
    ): LevyraUpdateArtifact? {
        val abiOrder = supportedAbis.map(::normalizeAbi)
        return artifacts.asSequence()
            .filter(LevyraUpdateArtifact::isApk)
            .filter { artifact -> sidecarSuffixes.none { artifact.name.lowercase(Locale.ROOT).endsWith(it) } }
            .filter { it.minSdk <= 0 || it.minSdk <= sdk }
            .map { artifact -> artifact to scoreArtifact(artifact, abiOrder, preferredHost) }
            .filter { it.second > Int.MIN_VALUE }
            .sortedWith(compareByDescending<Pair<LevyraUpdateArtifact, Int>> { it.second }.thenBy { it.first.name })
            .map(Pair<LevyraUpdateArtifact, Int>::first)
            .firstOrNull()
    }

    fun inferAbi(name: String): String {
        val normalized = name.lowercase(Locale.ROOT)
        return when {
            "arm64-v8a" in normalized || "arm64" in normalized || "aarch64" in normalized -> "arm64-v8a"
            "armeabi-v7a" in normalized || "armv7" in normalized -> "armeabi-v7a"
            "x86_64" in normalized || "x64" in normalized -> "x86_64"
            Regex("(^|[-_.])x86($|[-_.])").containsMatchIn(normalized) -> "x86"
            else -> "universal"
        }
    }

    fun rolloutBucket(deviceId: String, salt: String = "levyra"): Int {
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt\u001f$deviceId".toByteArray(Charsets.UTF_8))
        val positive = ((digest[0].toInt() and 0xff) shl 8) or (digest[1].toInt() and 0xff)
        return positive % 100
    }

    fun isEligibleForRollout(deviceId: String, rolloutPercent: Int, salt: String = "levyra"): Boolean {
        val safePercent = rolloutPercent.coerceIn(0, 100)
        return safePercent == 100 || (safePercent > 0 && rolloutBucket(deviceId, salt) < safePercent)
    }

    private fun scoreArtifact(artifact: LevyraUpdateArtifact, abiOrder: List<String>, preferredHost: String): Int {
        val abi = normalizeAbi(artifact.abi.ifBlank { inferAbi(artifact.name) })
        val abiIndex = abiOrder.indexOf(abi)
        val abiScore = when {
            abi == "universal" -> 300
            abiIndex >= 0 -> 1_000 - abiIndex * 50
            else -> return Int.MIN_VALUE
        }
        val releaseScore = if (artifact.name.contains("release", ignoreCase = true)) 80 else 0
        val universalPenalty = if (abi == "universal") -20 else 0
        val hostScore = if (preferredHost.isNotBlank() && artifact.downloadUrl.contains(preferredHost, ignoreCase = true)) 25 else 0
        return abiScore + releaseScore + universalPenalty + hostScore
    }

    private fun normalizeAbi(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
        "arm64", "aarch64" -> "arm64-v8a"
        "armv7", "armeabi" -> "armeabi-v7a"
        "x64", "amd64" -> "x86_64"
        "all", "any", "" -> "universal"
        else -> value.trim().lowercase(Locale.ROOT)
    }
}

object LevyraUpdateVerifier {
    fun verifyManifestSignature(
        manifest: LevyraUpdateManifest,
        publicKeyBase64: String
    ): LevyraResult<LevyraUpdateManifest> {
        if (manifest.signatureBase64.isBlank() || publicKeyBase64.isBlank()) {
            return LevyraResult.Failure(
                type = LevyraFailureType.SECURITY,
                retryable = false,
                message = "Missing manifest signature or public key"
            )
        }
        return runCatching {
            val publicKey = decodePublicKey(publicKeyBase64, manifest.signatureAlgorithm)
            val verifier = Signature.getInstance(manifest.signatureAlgorithm)
            verifier.initVerify(publicKey)
            verifier.update(manifest.canonicalPayload())
            if (!verifier.verify(Base64.getDecoder().decode(manifest.signatureBase64))) {
                throw SecurityException("Manifest signature mismatch")
            }
            LevyraResult.Success(
                value = manifest,
                source = LevyraDataSource.NETWORK,
                freshness = LevyraFreshness.FRESH
            )
        }.getOrElse { error ->
            LevyraResult.Failure(
                type = LevyraFailureType.SECURITY,
                retryable = false,
                message = error.message.orEmpty(),
                cause = error
            )
        }
    }

    fun verifyArtifact(
        input: InputStream,
        artifact: LevyraUpdateArtifact,
        inspection: LevyraArtifactInspection,
        expectedPackageName: String,
        expectedSignerSha256: String = "",
        publicKeyBase64: String = "",
        signatureAlgorithm: String = "SHA256withECDSA"
    ): LevyraResult<LevyraVerifiedArtifact> {
        if (inspection.packageName != expectedPackageName) {
            return failure("APK package name mismatch")
        }
        if (artifact.minSdk > 0 && inspection.sdk < artifact.minSdk) {
            return failure("APK requires SDK ${artifact.minSdk}", LevyraFailureType.UNSUPPORTED)
        }
        val artifactAbi = artifact.abi.trim().lowercase(Locale.ROOT)
        if (artifactAbi.isNotBlank() && artifactAbi != "universal" && artifactAbi != inspection.abi.lowercase(Locale.ROOT)) {
            return failure("APK ABI mismatch", LevyraFailureType.UNSUPPORTED)
        }
        if (expectedSignerSha256.isNotBlank() && !constantTimeEquals(expectedSignerSha256, inspection.signerSha256)) {
            return failure("APK signer mismatch")
        }

        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            val signature = if (artifact.signatureBase64.isNotBlank() && publicKeyBase64.isNotBlank()) {
                Signature.getInstance(signatureAlgorithm).apply {
                    initVerify(decodePublicKey(publicKeyBase64, signatureAlgorithm))
                }
            } else {
                null
            }
            var total = 0L
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                digest.update(buffer, 0, read)
                signature?.update(buffer, 0, read)
                total += read
            }
            if (artifact.sizeBytes > 0L && total != artifact.sizeBytes) throw SecurityException("APK size mismatch")
            val actualSha256 = digest.digest().toHex()
            if (artifact.sha256.isNotBlank() && !constantTimeEquals(artifact.sha256, actualSha256)) {
                throw SecurityException("APK SHA-256 mismatch")
            }
            if (signature != null && !signature.verify(Base64.getDecoder().decode(artifact.signatureBase64))) {
                throw SecurityException("APK signature mismatch")
            }
            LevyraResult.Success(
                value = LevyraVerifiedArtifact(artifact, inspection),
                source = LevyraDataSource.LOCAL,
                freshness = LevyraFreshness.FRESH,
                metadata = mapOf("sha256" to actualSha256, "sizeBytes" to total.toString())
            )
        }.getOrElse { error ->
            LevyraResult.Failure(
                type = LevyraFailureType.SECURITY,
                retryable = false,
                message = error.message.orEmpty(),
                cause = error
            )
        }
    }

    private fun decodePublicKey(base64: String, signatureAlgorithm: String): PublicKey {
        val keyAlgorithm = when {
            signatureAlgorithm.contains("Ed25519", ignoreCase = true) -> "Ed25519"
            signatureAlgorithm.contains("RSA", ignoreCase = true) -> "RSA"
            else -> "EC"
        }
        return KeyFactory.getInstance(keyAlgorithm).generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(base64)))
    }

    private fun constantTimeEquals(expected: String, actual: String): Boolean {
        val left = expected.trim().lowercase(Locale.ROOT).toByteArray(Charsets.US_ASCII)
        val right = actual.trim().lowercase(Locale.ROOT).toByteArray(Charsets.US_ASCII)
        return MessageDigest.isEqual(left, right)
    }

    private fun failure(message: String, type: LevyraFailureType = LevyraFailureType.SECURITY): LevyraResult.Failure =
        LevyraResult.Failure(type = type, retryable = false, message = message)

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }
}
