package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
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
