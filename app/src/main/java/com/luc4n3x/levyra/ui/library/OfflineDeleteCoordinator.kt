package com.luc4n3x.levyra.ui.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.viewmodel.LibraryViewModel

@Composable
internal fun rememberOfflineDeleteHandler(
    viewModel: LibraryViewModel
): (List<DownloadedTrack>) -> Unit {
    val context = LocalContext.current
    var pendingSystemDelete by remember { mutableStateOf<List<DownloadedTrack>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingSystemDelete
        pendingSystemDelete = emptyList()
        if (result.resultCode == Activity.RESULT_OK && pending.isNotEmpty()) {
            viewModel.deleteDownloads(pending)
        }
    }

    return { downloads ->
        val unique = downloads.distinctBy { it.id }
        if (unique.isNotEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                viewModel.deleteDownloads(unique)
            } else {
                val consentRequired = unique.filter { requiresMediaStoreDeleteConsent(context, it) }
                val consentIds = consentRequired.mapTo(hashSetOf()) { it.id }
                val direct = unique.filterNot { it.id in consentIds }
                if (direct.isNotEmpty()) {
                    viewModel.deleteDownloads(direct)
                }
                if (consentRequired.isNotEmpty()) {
                    val uris = consentRequired.mapNotNull(::mediaStoreDeleteUri).distinct()
                    if (uris.isEmpty()) {
                        viewModel.deleteDownloads(consentRequired)
                    } else {
                        val request = runCatching {
                            MediaStore.createDeleteRequest(context.contentResolver, uris)
                        }.getOrNull()
                        if (request != null) {
                            pendingSystemDelete = consentRequired
                            launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                        } else {
                            viewModel.deleteDownloads(consentRequired)
                        }
                    }
                }
            }
        }
    }
}

private fun requiresMediaStoreDeleteConsent(context: Context, download: DownloadedTrack): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    val uri = mediaStoreDeleteUri(download) ?: return false
    return context.checkUriPermission(
        uri,
        Process.myPid(),
        Process.myUid(),
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    ) != PackageManager.PERMISSION_GRANTED
}

private fun mediaStoreDeleteUri(download: DownloadedTrack): Uri? {
    val uri = runCatching { Uri.parse(download.uri) }.getOrNull() ?: return null
    if (!uri.scheme.equals("content", ignoreCase = true)) return null
    if (uri.authority != MediaStore.AUTHORITY) return null
    return uri
}
