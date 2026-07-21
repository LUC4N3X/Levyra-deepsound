package com.luc4n3x.levyra

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.ui.LevyraApp
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val pipMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOrientationPolicy()
        configureFastImageLoader()
        requestNotificationPermission()
        requestLegacyStoragePermission()
        requestUnrestrictedBatteryUsage()
        val startPalette = LevyraThemes.byId(LevyraThemes.APPLE_MUSIC)
        LevyraThemeController.apply(startPalette.id)
        WindowCompat.enableEdgeToEdge(window)
        window.setBackgroundDrawable(ColorDrawable(if (startPalette.isLight) Color.WHITE else Color.BLACK))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = startPalette.isLight
            isAppearanceLightNavigationBars = startPalette.isLight
        }
        LevyraLaunchActions.consumeFrom(intent)
        if (Build.VERSION.SDK_INT >= 28) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        requestHighRefreshRate()
        LevyraPipBridge.bind(
            enter = ::enterPictureInPicture,
            update = ::updatePictureInPictureParams
        )
        setContent {
            LevyraTheme {
                val viewModel: LevyraViewModel = viewModel()
                LevyraApp(
                    viewModel = viewModel,
                    isInPictureInPicture = pipMode.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        LevyraLaunchActions.consumeFrom(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyOrientationPolicy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.R && LevyraPipBridge.current().playing) {
            enterPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode.value = isInPictureInPictureMode
        LevyraPipBridge.updatePictureInPictureMode(isInPictureInPictureMode)
        applyOrientationPolicy()
    }

    override fun onDestroy() {
        LevyraPipBridge.unbind()
        super.onDestroy()
    }

    private fun requestUnrestrictedBatteryUsage() {
        val powerManager = getSystemService(android.os.PowerManager::class.java) ?: return
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return
        val prefs = getSharedPreferences("levyra_battery_guard", MODE_PRIVATE)
        if (prefs.getBoolean("requested", false)) return
        prefs.edit().putBoolean("requested", true).apply()
        runCatching {
            startActivity(
                Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(android.net.Uri.parse("package:$packageName"))
            )
        }
    }

    private fun enterPictureInPicture(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        val state = LevyraPipBridge.current()
        if (!state.canEnter) return false
        val params = buildPictureInPictureParams(state)
        setPictureInPictureParams(params)
        return try {
            enterPictureInPictureMode(params)
        } catch (_: IllegalArgumentException) {
            false
        } catch (_: IllegalStateException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun updatePictureInPictureParams(state: LevyraPipBridge.State) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        setPictureInPictureParams(buildPictureInPictureParams(state))
    }

    private fun buildPictureInPictureParams(state: LevyraPipBridge.State): PictureInPictureParams {
        val aspect = state.aspectRatio.coerceIn(0.42f, 2.39f)
        val denominator = 1000
        val numerator = (aspect * denominator).roundToInt().coerceAtLeast(1)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(numerator, denominator))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder
                .setAutoEnterEnabled(state.videoMode && state.playing)
                .setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun applyOrientationPolicy() {
        requestedOrientation = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            resources.configuration.smallestScreenWidthDp >= 600 -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private fun requestLegacyStoragePermission() {
        if (Build.VERSION.SDK_INT > 28) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1002)
        }
    }

    private fun configureFastImageLoader() {
        LevyraArtworkCache.configure(this)
    }

    private fun requestHighRefreshRate() {
        window.decorView.doOnAttach { decorView ->
            val mode = decorView.display?.supportedModes?.maxByOrNull { it.refreshRate } ?: return@doOnAttach
            val params = window.attributes
            params.preferredDisplayModeId = mode.modeId
            window.attributes = params
        }
    }
}
