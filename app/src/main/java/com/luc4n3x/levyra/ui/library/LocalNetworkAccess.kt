package com.luc4n3x.levyra.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
private const val LOCAL_NETWORK_PERMISSION_SDK = 37

@Stable
internal class LocalNetworkAccessState(
    granted: Boolean,
    private val launch: () -> Unit,
    private val openSettings: () -> Unit
) {
    var granted by mutableStateOf(granted)
        internal set
    internal var permanentlyDenied by mutableStateOf(false)
    internal var pending: ((Boolean) -> Unit)? = null

    fun ensure(onResult: (Boolean) -> Unit) {
        if (granted) {
            onResult(true)
            return
        }
        if (permanentlyDenied) {
            openSettings()
            onResult(false)
            return
        }
        pending = onResult
        launch()
    }
}

@Composable
internal fun rememberLocalNetworkAccess(): LocalNetworkAccessState {
    val context = LocalContext.current
    var holder: LocalNetworkAccessState? = null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val state = holder ?: return@rememberLauncherForActivityResult
        state.granted = granted
        if (!granted) {
            val activity = context.findActivity()
            state.permanentlyDenied = activity != null && !activity.shouldShowRequestPermissionRationale(ACCESS_LOCAL_NETWORK)
        }
        state.pending?.invoke(granted)
        state.pending = null
    }
    val currentLauncher by rememberUpdatedState(launcher)
    val state = remember {
        LocalNetworkAccessState(
            granted = hasLocalNetworkAccess(context),
            launch = { currentLauncher.launch(ACCESS_LOCAL_NETWORK) },
            openSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }
    holder = state
    LifecycleResumeEffect(state) {
        state.granted = hasLocalNetworkAccess(context)
        onPauseOrDispose { }
    }
    return state
}

private fun hasLocalNetworkAccess(context: Context): Boolean =
    Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_SDK ||
        ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
