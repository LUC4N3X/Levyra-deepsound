package com.luc4n3x.levyra

import android.Manifest
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraBackupManager
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.feature.recognition.LevyraRecognitionCenter
import com.luc4n3x.levyra.feature.recognition.MusicRecognitionService
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.runtime.RuntimeHooks
import com.luc4n3x.levyra.ui.LevyraApp
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.support.RemoteAnnouncementGate
import com.luc4n3x.levyra.ui.support.RemoteAnnouncementPromptPolicy
import com.luc4n3x.levyra.ui.support.SupportLevyraSettingsCard
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import com.luc4n3x.levyra.ui.update.LevyraUpdateBanner
import com.luc4n3x.levyra.ui.update.LevyraUpdatePhase
import com.luc4n3x.levyra.ui.update.LevyraUpdateScreen
import com.luc4n3x.levyra.update.AppUpdateContract
import com.luc4n3x.levyra.update.AppUpdateInstaller
import com.luc4n3x.levyra.update.AppUpdateSpeedTracker
import com.luc4n3x.levyra.update.PreparedAppUpdate
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber


private data class MainActivityUiSlice(
    val fontPreset: LevyraFontPreset,
    val isPlaying: Boolean,
    val showSettings: Boolean,
    val showOnboarding: Boolean,
    val languageCode: String,
    val recentListenCount: Int,
    val updateInfo: AppUpdateInfo?,
    val showUpdatePrompt: Boolean
)

private fun LevyraUiState.toMainActivityUiSlice(): MainActivityUiSlice = MainActivityUiSlice(
    fontPreset = interfaceSettings.fontPreset,
    isPlaying = isPlaying,
    showSettings = showSettings,
    showOnboarding = showOnboarding,
    languageCode = languageCode,
    recentListenCount = recentListens.size,
    updateInfo = updateInfo,
    showUpdatePrompt = showUpdatePrompt
)

class MainActivity : ComponentActivity() {
    private val pipMode = mutableStateOf(false)
    private val viewModel: LevyraViewModel by viewModels()
    private val updatePhase = mutableStateOf<LevyraUpdatePhase>(LevyraUpdatePhase.Idle)
    private val updateSpeedTracker = AppUpdateSpeedTracker()
    private val updateInstaller by lazy { AppUpdateInstaller(applicationContext) }
    private var updateJob: Job? = null
    private var updateRequestToken = 0L
    private var pendingUpdate: PreparedAppUpdate? = null

    private val unknownSourcesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        resumePendingUpdateInstall()
    }

    private val recognitionPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        LevyraLaunchActions.pendingShortcut.value = LevyraLaunchActions.SHORTCUT_SEARCH
        if (granted) startMicrophoneRecognitionService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOrientationPolicy()
        configureFastImageLoader()
        requestNotificationPermission()
        requestLegacyStoragePermission()
        val startPalette = LevyraThemes.byId(LevyraThemes.APPLE_MUSIC)
        LevyraThemeController.apply(startPalette.id)
        WindowCompat.enableEdgeToEdge(window)
        window.setBackgroundDrawable(ColorDrawable(if (startPalette.isLight) Color.WHITE else Color.BLACK))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = startPalette.isLight
            isAppearanceLightNavigationBars = startPalette.isLight
        }
        LevyraLaunchActions.consumeFrom(intent)
        handleRecognitionLaunchRequest()
        if (Build.VERSION.SDK_INT >= 28) {
            val params = window.attributes
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = params
        }
        LevyraPipBridge.bind(
            enter = ::enterPictureInPicture,
            update = ::updatePictureInPictureParams
        )
        val initialActivityUiState = viewModel.state.value.toMainActivityUiSlice()
        setContent {
            val activityStateFlow = remember(viewModel) {
                viewModel.state
                    .map(LevyraUiState::toMainActivityUiSlice)
                    .distinctUntilChanged()
            }
            val activityUiState by activityStateFlow.collectAsStateWithLifecycle(
                initialValue = initialActivityUiState
            )

            LevyraTheme(fontPreset = activityUiState.fontPreset) {
                var listenedPlaybackMs by rememberSaveable { mutableLongStateOf(0L) }

                LaunchedEffect(activityUiState.isPlaying) {
                    if (!activityUiState.isPlaying) return@LaunchedEffect
                    var lastTickMs = SystemClock.elapsedRealtime()
                    while (true) {
                        delay(1_000L)
                        val nowMs = SystemClock.elapsedRealtime()
                        listenedPlaybackMs += (nowMs - lastTickMs).coerceAtLeast(0L)
                        lastTickMs = nowMs
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        LevyraApp(
                            viewModel = viewModel,
                            isInPictureInPicture = pipMode.value
                        )
                    }
                    if (activityUiState.showSettings && !pipMode.value) {
                        SupportLevyraSettingsCard(
                            languageCode = activityUiState.languageCode,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        )
                    }
                }
                if (activityUiState.showSettings && !pipMode.value) {
                    RuntimeHooks.internalPanelOverlay()
                }
                RemoteAnnouncementGate(
                    enabled = !activityUiState.showOnboarding && !pipMode.value && !activityUiState.showSettings,
                    languageCode = activityUiState.languageCode,
                    hasPositiveListeningMoment = RemoteAnnouncementPromptPolicy.hasPositiveListeningMoment(
                        recentListenCount = activityUiState.recentListenCount,
                        listenedPlaybackMs = listenedPlaybackMs
                    )
                )
                if (
                    BuildConfig.UPSTREAM_UPDATES_ENABLED &&
                    !pipMode.value &&
                    !activityUiState.showOnboarding
                ) {
                    val updateStrings = LevyraStrings.forCode(activityUiState.languageCode)
                    val phase = updatePhase.value
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        AnimatedVisibility(
                            visible = phase !is LevyraUpdatePhase.Idle,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 }
                        ) {
                            LevyraUpdateBanner(
                                phase = phase,
                                strings = updateStrings,
                                onUpdate = {
                                    activityUiState.updateInfo?.let(::startUpdateFromBanner)
                                },
                                onCancel = ::cancelInAppUpdate,
                                onRetry = ::retryInAppUpdate,
                                onDismiss = ::dismissUpdateBanner,
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                    val available = activityUiState.updateInfo
                        ?.takeIf { activityUiState.showUpdatePrompt && it.isNewer }
                        ?.takeIf { phase is LevyraUpdatePhase.Idle }
                    AnimatedVisibility(
                        visible = available != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        available?.let { info ->
                            BackHandler { viewModel.dismissUpdatePrompt() }
                            LevyraUpdateScreen(
                                update = info,
                                strings = updateStrings,
                                languageCode = activityUiState.languageCode,
                                onUpdate = { startUpdateFromBanner(info) },
                                onLater = viewModel::dismissUpdatePrompt
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        LevyraLaunchActions.consumeFrom(intent)
        handleRecognitionLaunchRequest()
    }

    private fun handleRecognitionLaunchRequest() {
        if (LevyraLaunchActions.pendingShortcut.value != LevyraLaunchActions.SHORTCUT_RECOGNITION) return
        val permissionRequest = intent.getBooleanExtra(
            LevyraLaunchActions.EXTRA_RECOGNITION_PERMISSION_REQUEST,
            false
        )
        intent.removeExtra(LevyraLaunchActions.EXTRA_RECOGNITION_PERMISSION_REQUEST)

        LevyraLaunchActions.pendingShortcut.value = LevyraLaunchActions.SHORTCUT_SEARCH
        if (!LevyraRecognitionCenter.isAvailable || !permissionRequest) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            recognitionPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startMicrophoneRecognitionService()
        }
    }

    private fun startMicrophoneRecognitionService() {
        runCatching {
            ContextCompat.startForegroundService(this, MusicRecognitionService.microphoneIntent(this))
        }.onFailure { Timber.w(it, "Recognition service could not start") }
    }

    override fun startActivity(intent: Intent) {
        if (handleInAppUpdateIntent(intent)) return
        super.startActivity(intent)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        if (handleInAppUpdateIntent(intent)) return
        super.startActivity(intent, options)
    }

    private fun handleInAppUpdateIntent(intent: Intent): Boolean {
        if (!AppUpdateContract.matches(intent)) return false
        if (BuildConfig.UPSTREAM_UPDATES_ENABLED) beginInAppUpdate()
        return true
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
        updateJob?.cancel()
        LevyraPipBridge.unbind()
        super.onDestroy()
    }

    private fun beginInAppUpdate() {
        if (!BuildConfig.UPSTREAM_UPDATES_ENABLED || updateJob?.isActive == true) return
        val fallbackVersion = viewModel.state.value.updateInfo?.latestVersionName.orEmpty()
        updateSpeedTracker.reset()
        val requestToken = ++updateRequestToken
        updatePhase.value = LevyraUpdatePhase.Preparing(fallbackVersion)
        val startedAtMs = SystemClock.elapsedRealtime()
        lateinit var job: Job
        job = lifecycleScope.launch {
            try {
                runCatching {
                    val backupSettings = viewModel.state.value.backupSettings
                    if (backupSettings.preUpdate) {
                        LevyraBackupManager(applicationContext).exportAutomatic(backupSettings.retentionCount)
                    }
                }.onFailure { error -> Timber.w(error, "Pre-update backup failed") }
                val prepared = updateInstaller.prepareLatestUpdate { versionName, downloaded, total ->
                    val nowMs = SystemClock.elapsedRealtime()
                    val speed = updateSpeedTracker.sample(downloaded, nowMs)
                    runOnUiThread {
                        if (updateRequestToken != requestToken) return@runOnUiThread
                        updatePhase.value = LevyraUpdatePhase.Downloading(
                            versionName = versionName,
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            bytesPerSecond = speed,
                            elapsedMs = nowMs - startedAtMs
                        )
                    }
                }
                pendingUpdate = prepared
                updatePhase.value = LevyraUpdatePhase.Ready(prepared.versionName)
                requestPackageInstall(prepared)
            } catch (cancelled: CancellationException) {
                updatePhase.value = LevyraUpdatePhase.Idle
                throw cancelled
            } catch (error: Throwable) {
                Timber.w(error, "In-app update failed")
                pendingUpdate = null
                updatePhase.value = LevyraUpdatePhase.Failed(fallbackVersion)
            } finally {
                if (updateJob === job) updateJob = null
            }
        }
        updateJob = job
    }

    private fun cancelInAppUpdate() {
        val job = updateJob
        updateJob = null
        updateRequestToken++
        pendingUpdate = null
        updateSpeedTracker.reset()
        updatePhase.value = LevyraUpdatePhase.Idle
        job?.cancel()
    }

    private fun startUpdateFromBanner(update: AppUpdateInfo) {
        if (AppUpdateContract.matches(Intent.ACTION_VIEW, update.downloadUrl)) {
            beginInAppUpdate()
            return
        }
        val target = update.downloadUrl.trim().takeIf { it.startsWith("https://", ignoreCase = true) }
        if (target == null) {
            showUpdateFailure()
            return
        }
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
            .onFailure {
                Timber.w(it, "Unable to open update page")
                showUpdateFailure()
            }
    }

    private fun dismissUpdateBanner() {
        when (updatePhase.value) {
            is LevyraUpdatePhase.Idle,
            is LevyraUpdatePhase.Available -> viewModel.dismissUpdatePrompt()
            else -> {
                pendingUpdate = null
                updatePhase.value = LevyraUpdatePhase.Idle
                viewModel.dismissUpdatePrompt()
            }
        }
    }

    private fun retryInAppUpdate() {
        val prepared = pendingUpdate
        if (prepared != null) {
            requestPackageInstall(prepared)
            return
        }
        updatePhase.value = LevyraUpdatePhase.Idle
        beginInAppUpdate()
    }

    private fun requestPackageInstall(prepared: PreparedAppUpdate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            pendingUpdate = prepared
            updatePhase.value = LevyraUpdatePhase.PermissionRequired(prepared.versionName)
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            )
            runCatching { unknownSourcesLauncher.launch(settingsIntent) }
                .onFailure {
                    Timber.w(it, "Unable to open unknown-source settings")
                    pendingUpdate = null
                    updatePhase.value = LevyraUpdatePhase.Failed(prepared.versionName)
                }
            return
        }
        launchPackageInstaller(prepared)
    }

    private fun resumePendingUpdateInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            updatePhase.value = pendingUpdate
                ?.let { LevyraUpdatePhase.PermissionRequired(it.versionName) }
                ?: LevyraUpdatePhase.Idle
            return
        }
        val prepared = pendingUpdate
        if (prepared == null) {
            if (BuildConfig.UPSTREAM_UPDATES_ENABLED) beginInAppUpdate()
            return
        }
        launchPackageInstaller(prepared)
    }

    private fun launchPackageInstaller(prepared: PreparedAppUpdate) {
        val apkUri = runCatching {
            FileProvider.getUriForFile(this, "$packageName.update-files", prepared.apkFile)
        }.getOrElse {
            Timber.w(it, "Unable to expose update APK")
            pendingUpdate = null
            updatePhase.value = LevyraUpdatePhase.Failed(prepared.versionName)
            return
        }
        updatePhase.value = LevyraUpdatePhase.Installing(prepared.versionName)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            clipData = ClipData.newRawUri("Levyra update", apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(installIntent) }
            .onSuccess { updatePhase.value = LevyraUpdatePhase.Idle }
            .onFailure {
                Timber.w(it, "Unable to launch Android package installer")
                updatePhase.value = LevyraUpdatePhase.Failed(prepared.versionName)
            }
        pendingUpdate = null
    }

    private fun showUpdateFailure() {
        val strings = LevyraStrings.forCode(viewModel.state.value.languageCode)
        Toast.makeText(this, strings.cannotOpenDownload, Toast.LENGTH_LONG).show()
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
}
