package com.luc4n3x.levyra.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

internal fun crashReportShareIntent(context: Context, report: CrashReport, file: File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share-files", file)
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, report.title)
        clipData = ClipData.newRawUri("Levyra crash report", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
