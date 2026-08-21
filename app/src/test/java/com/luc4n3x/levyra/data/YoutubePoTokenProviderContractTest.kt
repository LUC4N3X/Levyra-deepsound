package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePoTokenProviderContractTest {
    @Test
    fun extractorProviderMintsOnlyTheVisitorBoundPlayerToken() {
        val provider = readSource("LevyraYoutubeSessionPoTokenProvider.kt")
        val security = readSource("YoutubePlaybackSecurity.kt")

        assertTrue(provider.contains("security.playerPoTokenRequired(session)"))
        assertFalse(provider.contains("poTokensRequired(session.visitorData"))
        assertTrue(security.contains("suspend fun playerPoTokenRequired(session: YoutubeGuestSession): String"))
        assertTrue(security.contains("tokenGenerator.playerToken("))
    }

    private fun readSource(name: String): String =
        Files.readString(sourceFile(name)).replace("\r\n", "\n")

    private fun sourceFile(name: String): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/data/$name"),
        Path.of("src/main/java/com/luc4n3x/levyra/data/$name")
    ).firstOrNull(Files::exists) ?: error("$name not found")
}
