package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.app.DesktopPlatform
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopUpdateChecksumTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun validChecksumKeepsInstaller() = runTest {
        val bytes = "verified installer".toByteArray()
        val installer = installerFile(bytes)
        val controller = controller(200, "${sha256(bytes)}  installer.msi")

        controller.verifyChecksum(release(), installer)

        assertTrue(Files.isRegularFile(installer))
        controller.shutdown()
    }

    @Test
    fun mismatchedChecksumDeletesInstaller() = runTest {
        val installer = installerFile("installer".toByteArray())
        val controller = controller(200, "0".repeat(64))

        assertVerificationFailsAndDeletes(controller, installer)
    }

    @Test
    fun malformedChecksumDeletesInstaller() = runTest {
        val installer = installerFile("installer".toByteArray())
        val controller = controller(200, "not-a-sha256")

        assertVerificationFailsAndDeletes(controller, installer)
    }

    @Test
    fun unsuccessfulChecksumResponseDeletesInstaller() = runTest {
        val installer = installerFile("installer".toByteArray())
        val controller = controller(503, "temporarily unavailable")

        assertVerificationFailsAndDeletes(controller, installer)
    }

    @Test
    fun startupSelectsLegacyReleaseAndRejectsInvalidInstaller() = assertStartupUpdate(sharedTag = false)

    @Test
    fun startupSelectsSharedReleaseAndRejectsInvalidInstaller() = assertStartupUpdate(sharedTag = true)

    private fun assertStartupUpdate(sharedTag: Boolean) = runTest {
        val target = DesktopPlatform.updateTarget() ?: return@runTest
        val prefix = if (sharedTag) "v" else target.tagPrefix
        val tag = "${prefix}3.2.5"
        val name = target.assetName("3.2.5")
        val installer = "test installer".toByteArray()
        val downloadUrl = "https://github.com/LUC4N3X/Levyra-deepsound/releases/download/$tag"
        val releases = buildJsonArray {
            add(buildJsonObject {
                put("tag_name", tag)
                put("published_at", "2026-11-26T00:00:00Z")
                put("assets", buildJsonArray {
                    for ((asset, size) in listOf(name to installer.size, "$name.sha256" to 64)) {
                        add(buildJsonObject {
                            put("name", asset)
                            put("browser_download_url", "$downloadUrl/$asset")
                            put("size", size)
                        })
                    }
                })
            })
        }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = when (request.url.pathSegments.last()) {
                "releases" -> releases.toString().toResponseBody()
                name -> installer.toResponseBody()
                "$name.sha256" -> "0".repeat(64).toResponseBody()
                else -> error("Unexpected update request")
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("test").body(body).build()
        }.build()
        val paths = AppPaths(temporaryFolder.root.toPath()).prepare()
        val controller = DesktopUpdateController(this, paths, "1.3.0", client)
        try {
            controller.check()
            val available = controller.state.first { it.phase == DesktopUpdatePhase.AVAILABLE }
            assertEquals("3.2.5", available.release?.version)
            assertTrue((available.release?.publishedAtEpochMs ?: 0L) > 0)
            controller.install { fail("An unverified installer closed the app") }
            val failed = controller.state.first { it.phase == DesktopUpdatePhase.FAILED }
            assertTrue(failed.error.contains("SHA-256"))
            assertFalse(Files.exists(paths.root.resolve("updates/$name")))
            assertFalse(Files.exists(paths.root.resolve("updates/$name.part")))
        } finally {
            controller.shutdown()
        }
    }

    private suspend fun assertVerificationFailsAndDeletes(
        controller: DesktopUpdateController,
        installer: Path
    ) {
        try {
            controller.verifyChecksum(release(), installer)
            fail("Checksum verification should fail")
        } catch (_: IllegalStateException) {
        } finally {
            controller.shutdown()
        }
        assertFalse(Files.exists(installer))
    }

    private fun controller(code: Int, body: String): DesktopUpdateController {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body(body.toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()
        return DesktopUpdateController(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            paths = AppPaths(temporaryFolder.root.toPath()).prepare(),
            currentVersion = "1.0.0",
            baseClient = client
        )
    }

    private fun installerFile(bytes: ByteArray): Path {
        val path = temporaryFolder.newFile("installer-${System.nanoTime()}.msi").toPath()
        return Files.write(path, bytes)
    }

    private fun release(): DesktopRelease = DesktopRelease(
        version = "1.0.1",
        name = "Levyra Desktop 1.0.1",
        notes = "",
        installer = DesktopReleaseAsset(
            name = "LEVYRA-Windows-1.0.1-x64.msi",
            url = "https://github.com/LUC4N3X/Levyra-deepsound/releases/download/desktop-v1.0.1/LEVYRA-Windows-1.0.1-x64.msi",
            size = 0L
        ),
        checksum = DesktopReleaseAsset(
            name = "LEVYRA-Windows-1.0.1-x64.msi.sha256",
            url = "https://github.com/LUC4N3X/Levyra-deepsound/releases/download/desktop-v1.0.1/LEVYRA-Windows-1.0.1-x64.msi.sha256",
            size = 0L
        )
    )

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }
}
