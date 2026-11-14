package com.luc4n3x.levyra.desktop.app

import com.luc4n3x.levyra.desktop.app.state.PlaybackController
import com.luc4n3x.levyra.desktop.app.state.PlaybackUiState
import com.luc4n3x.levyra.desktop.core.catalog.CatalogRepository
import com.luc4n3x.levyra.desktop.core.model.*
import com.luc4n3x.levyra.desktop.core.sponsorblock.SponsorBlockRepository
import com.luc4n3x.levyra.desktop.core.storage.*
import com.luc4n3x.levyra.desktop.core.stream.*
import com.luc4n3x.levyra.desktop.player.*
import java.nio.file.Path
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.types.Variant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LinuxPlatformTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun updatesUseTheOperatingSystemAndDistributionWithoutCrossInstalling() {
        assertEquals("LEVYRA-Windows-1.3.0-x64.msi", DesktopUpdateTarget.detect("Windows 11", "amd64")?.assetName("1.3.0"))
        assertEquals("LEVYRA-Linux-1.3.0-x64.deb", DesktopUpdateTarget.detect("Linux", "amd64", "ID=ubuntu\nID_LIKE=debian")?.assetName("1.3.0"))
        assertEquals("LEVYRA-Linux-1.3.0-x64.rpm", DesktopUpdateTarget.detect("Linux", "x86_64", "ID=fedora")?.assetName("1.3.0"))
        assertEquals("desktop-linux-v", DesktopUpdateTarget.detect("Linux", "amd64", "ID=linuxmint\nID_LIKE=\"ubuntu debian\"")?.tagPrefix)
        assertNull(DesktopUpdateTarget.detect("Linux", "aarch64", "ID=ubuntu"))
        assertNull(DesktopUpdateTarget.detect("Linux", "amd64", "ID=arch"))
        assertNull(DesktopUpdateTarget.detect("Mac OS X", "amd64"))
    }

    @Test fun desktopEntryQuotesExecutableAndPreservesTheUriArgument() {
        val entry = LinuxDesktopIntegration.desktopEntry(temporary.root.toPath().resolve("Music città/Levyra"))
        assertTrue(entry.contains("Exec=\""))
        assertTrue(entry.contains("Music città"))
        assertTrue(entry.contains("\" %u\n"))
        assertTrue(entry.contains("NoDisplay=true"))
        assertTrue(entry.contains("MimeType=x-scheme-handler/levyra;"))
        assertThrows(IllegalArgumentException::class.java) { LinuxDesktopIntegration.desktopEntry(Path.of("bad\nLevyra")) }
    }

    @Test fun mprisProjectsOriginalMetadataAndControlsTheSharedSession() = runTest {
        val paths = AppPaths(temporary.newFolder().toPath()).prepare()
        val resolver = object : StreamResolver {
            override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio = error("No network expected")
            override fun invalidate(track: Track) = Unit
        }
        val controller = PlaybackController(backgroundScope, resolver, CatalogRepository(), SettingsStore.create(paths),
            LibraryStore.create(paths), SessionStore.create(paths), SponsorBlockRepository(), { error("No native calls expected") })
        var raised = 0
        val service = LinuxMprisService(controller, { raised++ }, {}, {}, StandardTestDispatcher(testScheduler))
        try {
            val track = Track("original / हिन्दी", "Original title", "Original artist", "Album", "", durationMs = 200_000)
            val queue = PlayerQueue(items = listOf(track), original = listOf(track), index = 0)
            val properties = service.playerProperties(PlaybackUiState(queue, PlaybackStatus.PAUSED, 1_234, 200_000, 60, true))
            assertEquals("Paused", properties["PlaybackStatus"]?.value)
            assertEquals(1_234_000L, properties["Position"]?.value)
            assertEquals(0.0, properties["Volume"]?.value)
            @Suppress("UNCHECKED_CAST")
            val metadata = properties["Metadata"]!!.value as Map<String, Variant<*>>
            assertEquals("Original title", metadata["xesam:title"]?.value)
            assertEquals(200_000_000L, metadata["mpris:length"]?.value)
            assertTrue(metadata["mpris:trackid"]?.value is DBusPath)
            service.Set(LinuxMprisService.PLAYER, "Volume", 0.25)
            service.Set(LinuxMprisService.PLAYER, "Rate", 1.5)
            service.Raise()
            runCurrent()
            assertEquals(25, controller.state.value.volume)
            assertEquals(1.5f, controller.state.value.speed)
            assertEquals(1, raised)
            assertThrows(DBusExecutionException::class.java) { service.Set(LinuxMprisService.PLAYER, "Volume", Double.NaN) }
            assertThrows(DBusExecutionException::class.java) { service.Set(LinuxMprisService.PLAYER, "Metadata", "wrong") }
        } finally { service.close(); controller.shutdown() }
    }
}
