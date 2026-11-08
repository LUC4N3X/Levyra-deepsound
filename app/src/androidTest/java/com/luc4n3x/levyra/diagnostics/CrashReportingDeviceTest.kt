package com.luc4n3x.levyra.diagnostics

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.BuildConfig
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrashReportingDeviceTest {
    @Test fun simulateFatalExceptionInDebugProcess() {
        assumeTrue(BuildConfig.DEBUG && InstrumentationRegistry.getArguments().getString("simulateFatalCrash") == "true")
        CrashReporting.updateContext("Debug crash simulation", "Default")
        Thread({
            throw IllegalStateException(
                "Diagnostic simulation token=test-crash-token https://example.invalid/private",
                IllegalArgumentException("Simulated cause")
            )
        }, "LevyraFatalCrashTest").apply { start(); join() }
        fail("A fatal exception must terminate its process")
    }

    @Test fun recoveredReportIsPrivateSanitizedAndClaimedOnlyOnce() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyFatalCrash") == "true")
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CrashReporting.store(context)
        val report = store.claimPending()
        assertNotNull(report)
        assertTrue(report!!.body.contains("LevyraFatalCrashTest"))
        assertTrue(report.body.contains("Simulated cause"))
        assertFalse(report.body.contains("test-crash-token"))
        assertFalse(report.body.contains("example.invalid"))
        assertNull(store.claimPending())
        assertTrue(store.reports().any { it.id == report.id })
    }
}
