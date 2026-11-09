package com.luc4n3x.levyra.diagnostics

import android.content.Context
import android.os.Build
import android.os.Process
import androidx.media3.common.util.UnstableApi
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.player.PlaybackDiagnosticHistory
import com.luc4n3x.levyra.player.redactDiagnosticText
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

@UnstableApi
internal object CrashReporting {
    @Volatile private var installed = false
    @Volatile private var currentContext = CrashContext(
        screen = "Application startup", presentation = CrashPresentation(language = java.util.Locale.getDefault().toLanguageTag())
    )
    val presentation: CrashPresentation get() = currentContext.presentation
    private val reportScreens = AtomicInteger()
    private val sequence = AtomicInteger(Process.myPid())

    fun store(context: Context): CrashReportStore = CrashReportStore(File(context.filesDir, "crash-reports"))

    fun isDiagnosticsProcess(context: Context): Boolean {
        val name = if (Build.VERSION.SDK_INT >= 28) {
            android.app.Application.getProcessName()
        } else {
            context.getSystemService(android.app.ActivityManager::class.java).runningAppProcesses
                ?.firstOrNull { it.pid == Process.myPid() }?.processName
        }
        return name == "${context.packageName}:diagnostics"
    }

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        val storage = store(context)
        val environment = CrashEnvironment(
            version = BuildConfig.VERSION_NAME,
            build = "${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE}",
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL
        )
        val handler = FatalCrashHandler(
            previous = Thread.getDefaultUncaughtExceptionHandler(),
            capture = { thread, error ->
                if (!(reportScreens.get() > 0 && thread === android.os.Looper.getMainLooper().thread)) {
                    val now = System.currentTimeMillis()
                    storage.save(createCrashReport(
                        id = "$now-${sequence.incrementAndGet()}",
                        timestampMs = now,
                        thread = thread,
                        error = error,
                        environment = environment,
                        context = currentContext.copy(playback = PlaybackDiagnosticHistory.snapshot()),
                        redact = ::redactDiagnosticText
                    ))
                }
            },
            terminate = {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        )
        Thread.setDefaultUncaughtExceptionHandler(handler)
        installed = true
    }

    fun updateContext(screen: String, skin: String, presentation: CrashPresentation? = null) {
        currentContext = CrashContext(screen, skin, presentation = presentation ?: currentContext.presentation)
    }

    fun reportScreenOpened() { reportScreens.incrementAndGet() }
    fun reportScreenClosed() { reportScreens.decrementAndGet() }
}
