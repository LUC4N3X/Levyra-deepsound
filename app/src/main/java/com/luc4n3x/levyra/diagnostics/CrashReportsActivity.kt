package com.luc4n3x.levyra.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.ui.i18n.supportCopy
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrashReportsActivity : ComponentActivity() {
    private val viewModel by viewModels<CrashReportsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporting.reportScreenOpened()
        enableEdgeToEdge()
        viewModel.load(intent.getStringExtra(EXTRA_REPORT_ID)?.take(80), CrashPresentation.decode(intent.getStringExtra(EXTRA_PRESENTATION)))
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            androidx.compose.runtime.LaunchedEffect(state.presentation) {
                val light = applyCrashPresentation(state.presentation)
                androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = light
                    isAppearanceLightNavigationBars = light
                }
            }
            LevyraTheme(fontPreset = state.fontPreset, animationsEnabled = state.animationsEnabled) {
                CrashReportsScreen(
                    state = state, onSelect = viewModel::select, onDelete = viewModel::delete,
                    onCopy = { report -> copy(report.body) }, onShare = ::share,
                    onGithub = ::openGithub, onClose = ::closeReports
                )
            }
        }
    }

    override fun onDestroy() {
        CrashReporting.reportScreenClosed()
        super.onDestroy()
    }

    private fun copy(text: String): Boolean = try {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Levyra crash report", text))
        message(supportCopy(viewModel.state.value.language).copied)
        true
    } catch (_: Exception) {
        message(supportCopy(viewModel.state.value.language).failed)
        false
    }

    private fun openGithub(report: CrashReport) {
        lifecycleScope.launch {
            val draft = withContext(Dispatchers.Default) { crashIssueDraft(report) }
            if (!copy(draft.clipboard)) return@launch
            val words = supportCopy(viewModel.state.value.language)
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(draft.url)))
                message(if (draft.needsPaste) words.pasteOnGithub else words.githubReady)
            } catch (_: Exception) {
                message(words.failed)
            }
        }
    }

    private fun share(report: CrashReport) {
        lifecycleScope.launch {
            try {
                val file = viewModel.export(report)
                val send = crashReportShareIntent(this@CrashReportsActivity, report, file)
                startActivity(Intent.createChooser(send, supportCopy(viewModel.state.value.language).share))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message(supportCopy(viewModel.state.value.language).failed)
            }
        }
    }

    private fun closeReports() {
        if (intent.getBooleanExtra(EXTRA_CONTINUE_TO_APP, false)) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        finish()
    }

    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    internal companion object {
        const val EXTRA_REPORT_ID = "report_id"
        const val EXTRA_CONTINUE_TO_APP = "continue_to_app"
        const val EXTRA_PRESENTATION = "presentation"
    }
}
