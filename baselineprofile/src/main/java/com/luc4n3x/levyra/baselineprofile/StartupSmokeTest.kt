package com.luc4n3x.levyra.baselineprofile

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupSmokeTest {
    @Test
    fun appLaunchesAndRemainsAlive() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)

        device.executeShellCommand("am force-stop $PACKAGE_NAME")
        val launchResult = device.executeShellCommand(
            "am start -W -n $PACKAGE_NAME/.MainActivity"
        )

        assertFalse(
            "Android could not launch Levyra:\n$launchResult",
            launchResult.contains("Error", ignoreCase = true) ||
                launchResult.contains("Exception", ignoreCase = true)
        )
        assertTrue(
            "Levyra never presented an application window. Launch output:\n$launchResult",
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), STARTUP_TIMEOUT_MS)
        )

        SystemClock.sleep(PROCESS_STABILITY_WINDOW_MS)
        val processId = device.executeShellCommand("pidof $PACKAGE_NAME").trim()
        assertTrue(
            "Levyra process terminated immediately after startup. Launch output:\n$launchResult",
            processId.isNotBlank()
        )
    }

    private companion object {
        const val PACKAGE_NAME = "com.luc4n3x.levyra"
        const val STARTUP_TIMEOUT_MS = 20_000L
        const val PROCESS_STABILITY_WINDOW_MS = 4_000L
    }
}
