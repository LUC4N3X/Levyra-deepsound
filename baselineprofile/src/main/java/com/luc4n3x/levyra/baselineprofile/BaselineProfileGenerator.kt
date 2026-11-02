package com.luc4n3x.levyra.baselineprofile

import android.os.Build
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var previousLocales: String? = null

    @get:Rule
    val rule = BaselineProfileRule()

    @Before
    fun useCollectionLocale() {
        if (Build.VERSION.SDK_INT < 33) return
        val response = device.executeShellCommand("cmd locale get-app-locales com.luc4n3x.levyra")
        val tags = response.substringAfterLast('[', "").substringBefore(']')
        check(tags.matches(Regex("[A-Za-z0-9,-]*"))) { "Unexpected application locale response" }
        previousLocales = tags
        device.executeShellCommand("cmd locale set-app-locales com.luc4n3x.levyra --locales en")
    }

    @After
    fun restoreCollectionLocale() {
        val tags = previousLocales ?: return
        val locales = if (tags.isBlank()) "" else " --locales $tags"
        device.executeShellCommand("cmd locale set-app-locales com.luc4n3x.levyra$locales")
    }

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.luc4n3x.levyra",
            includeInStartupProfile = true
        ) {
            launchAndExerciseLevyra()
        }
    }
}
