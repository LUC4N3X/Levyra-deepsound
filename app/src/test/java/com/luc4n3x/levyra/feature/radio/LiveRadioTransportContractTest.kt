package com.luc4n3x.levyra.feature.radio

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRadioTransportContractTest {
    @Test
    fun publicLegacyHttpStreamsStayPlayableWithoutAllowingLocalTargets() {
        val manifest = readSource(
            "AndroidManifest.xml",
            Path.of("app/src/main/AndroidManifest.xml"),
            Path.of("src/main/AndroidManifest.xml")
        )
        val service = readSource(
            "PlaybackService.kt",
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/PlaybackService.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/PlaybackService.kt")
        )

        assertTrue(manifest.contains("android:usesCleartextTraffic=\"true\""))
        assertTrue(service.contains(".dns(RadioUrlPolicy.publicDns)"))
        assertTrue(service.contains("if (!RadioUrlPolicy.isAllowed(request.url.toString()))"))
        assertTrue(service.contains("if (scheme == \\\"http\\\")"))
        assertTrue(service.contains("Cleartext HTTP is only allowed for live radio"))
        assertTrue(RadioUrlPolicy.isAllowed("http://relay.181.fm:8098/"))
        assertFalse(RadioUrlPolicy.isAllowed("http://127.0.0.1:8098/"))
        assertFalse(RadioUrlPolicy.isAllowed("http://192.168.1.10:8098/"))
    }

    private fun readSource(name: String, vararg candidates: Path): String =
        candidates.firstOrNull(Files::exists)
            ?.let(Files::readString)
            ?.replace("\r\n", "\n")
            ?: error("$name not found")
}
