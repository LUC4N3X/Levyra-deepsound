package com.luc4n3x.levyra.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.AppUpdateRepository
import com.luc4n3x.levyra.data.InstallableAppUpdate
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.nexus.update.LevyraVersionComparator
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

internal object AppUpdateContract {
    const val INSTALL_URI = "levyra-internal://updates/install"

    fun matches(action: String?, uri: String?): Boolean =
        action == Intent.ACTION_VIEW && uri == INSTALL_URI

    fun matches(intent: Intent?): Boolean = matches(intent?.action, intent?.dataString)
}

internal data class PreparedAppUpdate(
    val versionName: String,
    val apkFile: File
)

internal const val MAX_UPDATE_APK_BYTES = 250L * 1024L * 1024L
private const val MAX_UPDATE_REDIRECTS = 5
private const val UPDATE_APK_FILE = "levyra-update.apk"
private const val UPDATE_APK_PART_FILE = "levyra-update.apk.part"
private const val LEVYRA_RELEASE_PATH_PREFIX = "/LUC4N3X/Levyra-deepsound/releases/download/"
private val UPDATE_REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

internal fun validateLevyraUpdateUrl(value: String, initial: Boolean): HttpUrl? {
    val url = value.trim().toHttpUrlOrNull() ?: return null
    if (!url.isHttps || url.port != 443) return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null) return null
    val host = url.host.lowercase(Locale.ROOT)
    if (!isAllowedUpdateHost(host)) return null
    if (initial && (host != "github.com" || !url.encodedPath.startsWith(LEVYRA_RELEASE_PATH_PREFIX))) return null
    return url
}

internal fun updateApkContentTypeAccepted(value: String?): Boolean {
    val mime = value
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    return mime.isBlank() || mime == "application/vnd.android.package-archive" ||
        mime == "application/octet-stream" || mime == "binary/octet-stream"
}

internal fun isPublicUpdateAddress(address: InetAddress): Boolean {
    if (
        address.isAnyLocalAddress ||
        address.isLoopbackAddress ||
        address.isLinkLocalAddress ||
        address.isSiteLocalAddress ||
        address.isMulticastAddress
    ) return false

    val bytes = address.address
    val ipv4 = when {
        bytes.size == 4 -> bytes
        bytes.size == 16 && bytes.take(10).all { it.toInt() == 0 } &&
            bytes[10].toInt() == -1 && bytes[11].toInt() == -1 -> bytes.copyOfRange(12, 16)
        else -> null
    }
    if (ipv4 != null) {
        val a = ipv4[0].toInt() and 0xff
        val b = ipv4[1].toInt() and 0xff
        val c = ipv4[2].toInt() and 0xff
        if (a == 0 || a == 10 || a == 127 || a >= 224) return false
        if (a == 100 && b in 64..127) return false
        if (a == 169 && b == 254) return false
        if (a == 172 && b in 16..31) return false
        if (a == 192 && b == 168) return false
        if (a == 192 && b == 0 && c in 0..2) return false
        if (a == 198 && b in 18..19) return false
        if (a == 198 && b == 51 && c == 100) return false
        if (a == 203 && b == 0 && c == 113) return false
        return true
    }

    if (bytes.size == 16) {
        val first = bytes[0].toInt() and 0xff
        if (first and 0xfe == 0xfc) return false
        if (
            first == 0x20 && (bytes[1].toInt() and 0xff) == 0x01 &&
            (bytes[2].toInt() and 0xff) == 0x0d && (bytes[3].toInt() and 0xff) == 0xb8
        ) return false
    }
    return true
}

internal fun CoroutineScope.launchUpdateCallCancellationWatcher(cancelCall: () -> Unit): Job =
    launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            cancelCall()
        }
    }

private fun isAllowedUpdateHost(host: String): Boolean =
    host == "github.com" || host == "githubusercontent.com" || host.endsWith(".githubusercontent.com")

internal class AppUpdateInstaller(
    context: Context,
    private val repository: AppUpdateRepository = AppUpdateRepository(context.applicationContext),
    httpClient: OkHttpClient = LevyraHttpClientFactory.general(context.applicationContext)
) {
    private val appContext = context.applicationContext
    private val baseDns = httpClient.dns
    private val downloadClient = httpClient.newBuilder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val host = hostname.lowercase(Locale.ROOT)
                if (!isAllowedUpdateHost(host)) throw UnknownHostException("Update host not allowed")
                val addresses = baseDns.lookup(hostname)
                if (addresses.isEmpty() || addresses.any { !isPublicUpdateAddress(it) }) {
                    throw UnknownHostException("Update destination is not public")
                }
                return addresses
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    suspend fun prepareLatestUpdate(onProgress: (String, Int?) -> Unit): PreparedAppUpdate = withContext(Dispatchers.IO) {
        if (!BuildConfig.UPSTREAM_UPDATES_ENABLED) {
            throw IllegalStateException("Upstream updates are disabled")
        }
        val update = repository.latestInstallable()
        val versionName = update.info.latestVersionName
        if (update.assetSizeBytes > MAX_UPDATE_APK_BYTES) throw IOException("Update APK is too large")

        val directory = File(appContext.cacheDir, "updates")
        if (!directory.exists() && !directory.mkdirs()) throw IOException("Unable to create update cache")
        val finalFile = File(directory, UPDATE_APK_FILE)
        val partFile = File(directory, UPDATE_APK_PART_FILE)
        partFile.delete()

        if (finalFile.isFile) {
            try {
                verifyDownloadedApk(finalFile, update)
                onProgress(versionName, 100)
                return@withContext PreparedAppUpdate(versionName, finalFile)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                finalFile.delete()
            }
        }

        val initialUrl = validateLevyraUpdateUrl(update.assetDownloadUrl, initial = true)
            ?: throw IOException("Invalid Levyra update URL")
        downloadToFile(initialUrl, update, partFile) { progress -> onProgress(versionName, progress) }
        if (!partFile.renameTo(finalFile)) {
            partFile.delete()
            throw IOException("Unable to finalize update APK")
        }
        try {
            verifyDownloadedApk(finalFile, update)
        } catch (cancelled: CancellationException) {
            finalFile.delete()
            throw cancelled
        } catch (error: Throwable) {
            finalFile.delete()
            throw error
        }
        onProgress(versionName, 100)
        PreparedAppUpdate(versionName, finalFile)
    }

    private suspend fun downloadToFile(
        initialUrl: HttpUrl,
        update: InstallableAppUpdate,
        destination: File,
        onProgress: (Int?) -> Unit
    ) {
        var currentUrl = initialUrl
        var redirects = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            val request = Request.Builder()
                .url(currentUrl)
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream;q=0.9")
                .header("User-Agent", "LEVYRA/${BuildConfig.VERSION_NAME}")
                .header("Cache-Control", "no-cache")
                .build()
            val call = downloadClient.newCall(request)
            val cancellationWatcher = CoroutineScope(currentCoroutineContext())
                .launchUpdateCallCancellationWatcher(call::cancel)
            val response = try {
                call.execute().also { currentCoroutineContext().ensureActive() }
            } catch (error: Throwable) {
                cancellationWatcher.cancel()
                destination.delete()
                currentCoroutineContext().ensureActive()
                throw error
            }
            try {
                if (response.code in UPDATE_REDIRECT_CODES) {
                    if (redirects >= MAX_UPDATE_REDIRECTS) throw IOException("Too many update redirects")
                    val location = response.header("Location") ?: throw IOException("Update redirect has no location")
                    val next = currentUrl.resolve(location)?.toString().orEmpty()
                    currentUrl = validateLevyraUpdateUrl(next, initial = false)
                        ?: throw IOException("Unsafe update redirect")
                    redirects++
                    continue
                }
                if (!response.isSuccessful) throw IOException("Update download failed (${response.code})")
                if (!updateApkContentTypeAccepted(response.header("Content-Type"))) {
                    throw IOException("Unexpected update content type")
                }
                val body = response.body
                val declaredLength = body.contentLength().takeIf { it >= 0L } ?: 0L
                if (declaredLength > MAX_UPDATE_APK_BYTES) throw IOException("Update APK is too large")
                if (update.assetSizeBytes > 0L && declaredLength > 0L && declaredLength != update.assetSizeBytes) {
                    throw IOException("Update size does not match release metadata")
                }

                val expectedLength = update.assetSizeBytes.takeIf { it > 0L } ?: declaredLength
                var total = 0L
                var lastProgress = -1
                if (expectedLength <= 0L) onProgress(null)
                body.byteStream().use { input ->
                    destination.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count == 0) continue
                            total += count
                            if (total > MAX_UPDATE_APK_BYTES) throw IOException("Update APK exceeded size limit")
                            output.write(buffer, 0, count)
                            if (expectedLength > 0L) {
                                val progress = ((total * 100L) / expectedLength).toInt().coerceIn(0, 99)
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    onProgress(progress)
                                }
                            }
                        }
                        output.flush()
                    }
                }
                if (total < 4_096L) throw IOException("Downloaded update is not a valid APK")
                if (update.assetSizeBytes > 0L && total != update.assetSizeBytes) {
                    throw IOException("Downloaded update size does not match release metadata")
                }
                if (declaredLength > 0L && total != declaredLength) {
                    throw IOException("Downloaded update is incomplete")
                }
                return
            } catch (cancelled: CancellationException) {
                destination.delete()
                throw cancelled
            } catch (error: Throwable) {
                destination.delete()
                currentCoroutineContext().ensureActive()
                throw error
            } finally {
                response.close()
                cancellationWatcher.cancel()
            }
        }
    }

    private fun verifyDownloadedApk(file: File, update: InstallableAppUpdate) {
        if (!file.isFile || file.length() < 4_096L || file.length() > MAX_UPDATE_APK_BYTES) {
            throw IOException("Update APK is invalid")
        }
        if (update.assetSizeBytes > 0L && file.length() != update.assetSizeBytes) {
            throw IOException("Update APK size does not match release metadata")
        }

        val packageManager = appContext.packageManager
        val installed = installedPackageInfo(packageManager)
        val archive = archivePackageInfo(packageManager, file)
            ?: throw IOException("Unable to inspect update APK")
        if (archive.packageName != appContext.packageName) throw IOException("Update package name mismatch")

        val installedVersionCode = packageVersionCode(installed)
        val archiveVersionCode = packageVersionCode(archive)
        if (archiveVersionCode <= installedVersionCode) throw IOException("Update APK is not newer")
        val archiveVersionName = archive.versionName.orEmpty()
        if (
            archiveVersionName.isNotBlank() &&
            LevyraVersionComparator.compare(archiveVersionName, update.info.latestVersionName) != 0
        ) {
            throw IOException("Update APK version does not match release metadata")
        }
        if (!hasTrustedSigningLineage(installed, archive)) throw IOException("Update signing certificate mismatch")
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfo(packageManager: PackageManager): PackageInfo {
        val flags = signingFlags()
        return if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            packageManager.getPackageInfo(appContext.packageName, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun archivePackageInfo(packageManager: PackageManager, file: File): PackageInfo? {
        val flags = signingFlags()
        return if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.PackageInfoFlags.of(flags.toLong())
            )
        } else {
            packageManager.getPackageArchiveInfo(file.absolutePath, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun packageVersionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun hasTrustedSigningLineage(installed: PackageInfo, archive: PackageInfo): Boolean {
        val installedSigners = if (Build.VERSION.SDK_INT >= 28) {
            installed.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            installed.signatures.orEmpty()
        }
        val archiveLineage = if (Build.VERSION.SDK_INT >= 28) {
            val signingInfo = archive.signingInfo ?: return false
            if (signingInfo.hasPastSigningCertificates()) {
                signingInfo.signingCertificateHistory
            } else {
                signingInfo.apkContentsSigners
            }.orEmpty()
        } else {
            archive.signatures.orEmpty()
        }
        if (installedSigners.isEmpty() || archiveLineage.isEmpty()) return false
        return installedSigners.all { installedSigner ->
            archiveLineage.any { archiveSigner -> signaturesEqual(installedSigner, archiveSigner) }
        }
    }

    private fun signaturesEqual(left: Signature, right: Signature): Boolean =
        left.toByteArray().contentEquals(right.toByteArray())
}
