package com.luc4n3x.levyra.desktop.app

import java.net.InetAddress
import java.net.Socket
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopInstanceManagerTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun secondLaunchForwardsAndRepeatedCloseCannotUnlockReplacement() = runBlocking {
        val root = temporary.newFolder().toPath()
        val first = DesktopInstanceManager.acquire(root = root)!!
        try {
            assertNull(DesktopInstanceManager.acquire("levyra://search?q=हिन्दी", root))
            assertEquals("levyra://search?q=हिन्दी", withTimeout(3_000) { first.requests.first() })
            first.close()
            val replacement = DesktopInstanceManager.acquire(root = root)!!
            try {
                first.close()
                assertNull(DesktopInstanceManager.acquire("show", root))
                assertEquals("show", withTimeout(3_000) { replacement.requests.first() })
            } finally { replacement.close() }
        } finally { first.close() }
    }

    @Test fun stalledClientCannotPermanentlyBlockSecondLaunch() = runBlocking {
        val root = temporary.newFolder().toPath()
        DesktopInstanceManager.acquire(root = root)!!.use { instance ->
            val port = Files.readString(root.resolve("desktop-instance.lock")).trim().toInt()
            Socket(InetAddress.getLoopbackAddress(), port).use {
                assertNull(DesktopInstanceManager.acquire("show", root))
                assertEquals("show", withTimeout(4_000) { instance.requests.first() })
            }
        }
    }
}
