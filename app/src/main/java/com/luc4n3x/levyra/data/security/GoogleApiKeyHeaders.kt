package com.luc4n3x.levyra.data.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.luc4n3x.levyra.BuildConfig
import okhttp3.Request
import java.net.HttpURLConnection
import java.security.MessageDigest

object GoogleApiKeyHeaders {
    fun applyTo(connection: HttpURLConnection, context: Context?) {
        connection.setRequestProperty("X-Android-Package", resolvePackageName(context))
        resolveCertificateSha1(context)?.let { connection.setRequestProperty("X-Android-Cert", it) }
    }

    fun applyTo(builder: Request.Builder, context: Context?): Request.Builder {
        builder.header("X-Android-Package", resolvePackageName(context))
        resolveCertificateSha1(context)?.let { builder.header("X-Android-Cert", it) }
        return builder
    }

    private fun resolvePackageName(context: Context?): String {
        return context?.packageName?.takeIf { it.isNotBlank() } ?: BuildConfig.APPLICATION_ID
    }

    @Suppress("DEPRECATION")
    private fun resolveCertificateSha1(context: Context?): String? {
        val appContext = context?.applicationContext ?: return null
        return runCatching {
            val packageName = resolvePackageName(appContext)
            val packageManager = appContext.packageManager
            val signatureBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo ?: return@runCatching null
                val signatures = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                signatures.firstOrNull()?.toByteArray()
            } else {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                info.signatures?.firstOrNull()?.toByteArray()
            } ?: return@runCatching null
            MessageDigest.getInstance("SHA-1")
                .digest(signatureBytes)
                .joinToString(separator = "") { byte -> "%02X".format(byte) }
        }.getOrNull()
    }
}
