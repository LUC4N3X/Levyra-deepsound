package com.luc4n3x.levyra.ui.artwork

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.MainActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LivingArtworkDeviceTest {
    @Test
    fun activeArtworkDrawsFramesOnSupportedAndroidVersions() {
        val frames = CountDownLatch(3)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    LivingArtworkLayer(
                        colors = livingArtworkColors(Color.Blue, Color.Magenta),
                        active = true,
                        modifier = Modifier.size(200.dp).drawWithContent {
                            drawContent()
                            frames.countDown()
                        }
                    )
                }
            }
            assertTrue("Active artwork must draw frames", frames.await(15, TimeUnit.SECONDS))
        }
    }
}
