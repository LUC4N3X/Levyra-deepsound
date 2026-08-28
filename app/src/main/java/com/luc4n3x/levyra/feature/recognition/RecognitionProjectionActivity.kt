package com.luc4n3x.levyra.feature.recognition

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import timber.log.Timber

class RecognitionProjectionActivity : ComponentActivity() {

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onConsentResult
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            finish()
            return
        }
        if (savedInstanceState != null) return
        val manager = getSystemService(MediaProjectionManager::class.java)
        if (manager == null) {
            finish()
            return
        }
        runCatching { consentLauncher.launch(manager.createScreenCaptureIntent()) }
            .onFailure {
                Timber.w(it, "Media projection consent could not be requested")
                finish()
            }
    }

    private fun onConsentResult(result: ActivityResult) {
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            runCatching {
                ContextCompat.startForegroundService(
                    this,
                    MusicRecognitionService.devicePlaybackIntent(this, result.resultCode, data)
                )
            }.onFailure { Timber.w(it, "Device playback recognition could not start") }
        }
        finish()
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, RecognitionProjectionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
