package com.luc4n3x.levyra.nexus.update

import com.luc4n3x.levyra.nexus.core.LevyraResult
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraUpdateCoreTest {
    @Test
    fun selectsNativeAbiBeforeUniversal() {
        val selected = LevyraUpdateSelector.selectArtifact(
            artifacts = listOf(
                LevyraUpdateArtifact("levyra-universal-release.apk", "https://a/u.apk", abi = "universal"),
                LevyraUpdateArtifact("levyra-arm64-v8a-release.apk", "https://a/a.apk", abi = "arm64-v8a")
            ),
            sdk = 35,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a")
        )
        assertEquals("levyra-arm64-v8a-release.apk", selected?.name)
    }

    @Test
    fun stableVersionBeatsPrereleaseWithSameNumbers() {
        assertTrue(LevyraVersionComparator.compare("2.3.16", "2.3.16-beta2") > 0)
        assertTrue(LevyraVersionComparator.compare("2.3.17-beta2", "2.3.16") > 0)
        assertTrue(LevyraVersionComparator.compare("2.3.16-beta2", "2.3.16-beta1") > 0)
        assertTrue(LevyraVersionComparator.compare("2.3.16-rc1", "2.3.16-beta9") > 0)
    }

    @Test
    fun infersAbiFromFilenameAndRejectsIncompatibleArtifacts() {
        val inferred = LevyraUpdateSelector.selectArtifact(
            artifacts = listOf(LevyraUpdateArtifact("levyra-arm64-v8a-release.apk", "https://a/a.apk")),
            sdk = 35,
            supportedAbis = listOf("arm64-v8a")
        )
        assertEquals("levyra-arm64-v8a-release.apk", inferred?.name)
        val incompatible = LevyraUpdateSelector.selectArtifact(
            artifacts = listOf(LevyraUpdateArtifact("levyra-arm64-v8a-release.apk", "https://a/a.apk")),
            sdk = 35,
            supportedAbis = listOf("x86_64")
        )
        assertEquals(null, incompatible)
    }

    @Test
    fun rolloutIsDeterministic() {
        val first = LevyraUpdateSelector.rolloutBucket("device-1")
        assertEquals(first, LevyraUpdateSelector.rolloutBucket("device-1"))
        assertFalse(LevyraUpdateSelector.isEligibleForRollout("device-1", 0))
        assertTrue(LevyraUpdateSelector.isEligibleForRollout("device-1", 100))
    }

    @Test
    fun verifiesManifestAndArtifact() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val bytes = "levyra-apk".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val artifactSignature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(bytes)
            Base64.getEncoder().encodeToString(sign())
        }
        val artifact = LevyraUpdateArtifact(
            name = "levyra-arm64-v8a-release.apk",
            downloadUrl = "https://example.com/levyra.apk",
            sizeBytes = bytes.size.toLong(),
            abi = "arm64-v8a",
            minSdk = 26,
            sha256 = hash,
            signatureBase64 = artifactSignature
        )
        val unsignedManifest = LevyraUpdateManifest(
            channel = LevyraUpdateChannel.STABLE,
            versionCode = 2_030_017L,
            versionName = "2.3.17",
            packageName = "com.luc4n3x.levyra",
            artifacts = listOf(artifact)
        )
        val manifestSignature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(unsignedManifest.canonicalPayload())
            Base64.getEncoder().encodeToString(sign())
        }
        val manifest = unsignedManifest.copy(signatureBase64 = manifestSignature)
        val publicKey = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        assertTrue(LevyraUpdateVerifier.verifyManifestSignature(manifest, publicKey) is LevyraResult.Success)
        val verified = LevyraUpdateVerifier.verifyArtifact(
            input = ByteArrayInputStream(bytes),
            artifact = artifact,
            inspection = LevyraArtifactInspection(
                packageName = "com.luc4n3x.levyra",
                signerSha256 = "signer",
                sdk = 35,
                abi = "arm64-v8a"
            ),
            expectedPackageName = "com.luc4n3x.levyra",
            publicKeyBase64 = publicKey
        )
        assertTrue(verified is LevyraResult.Success)
    }
}
