package com.luc4n3x.levyra.ui.jam

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.luc4n3x.levyra.feature.jam.JamSessionCode
import com.luc4n3x.levyra.feature.jam.JamSessionState
import com.luc4n3x.levyra.feature.jam.JamUiState
import com.luc4n3x.levyra.feature.jam.JamConnectionState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.jamScanQr

internal data class JamQrScanAction(val scan: () -> Unit, val unavailable: Boolean, val dismissFailure: () -> Unit)

@Composable
internal fun rememberJamQrScanAction(jam: JamUiState, strings: LevyraStrings, onOpen: () -> Unit, onJoin: (String) -> Unit): JamQrScanAction {
    var unavailable by rememberSaveable { mutableStateOf(false) }
    val scan = rememberJamQrScanner(!jam.isActive && jam.connection != JamConnectionState.Connecting, strings.jamScanQr, onJoin = {
        onOpen()
        onJoin(it)
    }, onUnavailable = {
        unavailable = true
        onOpen()
    })
    return JamQrScanAction(scan = { unavailable = false; scan() }, unavailable = unavailable, dismissFailure = { unavailable = false })
}

@Composable
internal fun rememberJamQrScanner(enabled: Boolean, prompt: String, onJoin: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentJoin by rememberUpdatedState(onJoin)
    val currentUnavailable by rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (currentEnabled) {
            val contents = result.contents
            when {
                result.originalIntent?.getBooleanExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, false) == true -> currentUnavailable()
                contents != null -> currentJoin(
                    contents.takeIf { it.length <= JamSessionState.MAX_TEXT_LENGTH }
                        ?.let(JamSessionCode::parse)?.deepLink().orEmpty()
                )
            }
        }
    }
    return {
        if (currentEnabled) {
            try {
                launcher.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt(prompt).setBeepEnabled(false).setBarcodeImageEnabled(false)
                    .setOrientationLocked(false).addExtra(Intents.Scan.SHOW_MISSING_CAMERA_PERMISSION_DIALOG, false))
            } catch (_: ActivityNotFoundException) {
                currentUnavailable()
            } catch (_: SecurityException) {
                currentUnavailable()
            }
        }
    }
}
