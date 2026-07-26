package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.app.AppInfo
import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

internal enum class DesktopUpdatePhase {
    IDLE,
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    INSTALLING,
    FAILED
}

internal data class DesktopReleaseAsset(
    val name: String,
    val url: String,
    val size: Long
)

internal data class DesktopRelease(
    val version: String,
    val name: String,
    val notes: String,
    val installer: DesktopReleaseAsset,
    val checksum: DesktopReleaseAsset
)

internal data class DesktopUpdateUiState(
    val phase: DesktopUpdatePhase = DesktopUpdatePhase.IDLE,
    val release: DesktopRelease? = null,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String = ""
) {
    val progress: Float
        get() = if (totalBytes > 0L) {
            (bytesDownloaded.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

internal class DesktopUpdateController(
    private val scope: CoroutineScope,
    private val paths: AppPaths,
    private val currentVersion: String = AppInfo.version(),
    baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    private val client = baseClient.newBuilder()
        .callTimeout(0L, TimeUnit.MILLISECONDS)
        .readTimeout(0L, TimeUnit.MILLISECONDS)
        .build()
    private val internalState = MutableStateFlow(DesktopUpdateUiState())
    private var checkJob: Job? = null
    private var installJob: Job? = null
    private var dismissedVersion: String = ""

    val state: StateFlow<DesktopUpdateUiState> = internalState.asStateFlow()

    fun check() {
        if (checkJob?.isActive == true || installJob?.isActive == true) return
        checkJob = scope.launch {
            internalState.value = DesktopUpdateUiState(phase = DesktopUpdatePhase.CHECKING)
            val release = try {
                withContext(Dispatchers.IO) { fetchLatestRelease() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                null
            }
            internalState.value = when {
                release == null -> DesktopUpdateUiState()
                release.version == dismissedVersion -> DesktopUpdateUiState()
                DesktopVersion.isNewer(release.version, currentVersion) -> DesktopUpdateUiState(
                    phase = DesktopUpdatePhase.AVAILABLE,
                    release = release
                )
                else -> DesktopUpdateUiState()
            }
        }
    }

    fun dismiss() {
        dismissedVersion = internalState.value.release?.version.orEmpty()
        internalState.value = DesktopUpdateUiState()
    }

    fun install(onReadyToExit: () -> Unit) {
        val release = internalState.value.release ?: return
        if (installJob?.isActive == true) return
        installJob = scope.launch {
            try {
                internalState.value = DesktopUpdateUiState(
                    phase = DesktopUpdatePhase.DOWNLOADING,
                    release = release,
                    totalBytes = release.installer.size
                )
                val installer = downloadInstaller(release)
                launchInstaller(release, installer)
                internalState.update { it.copy(phase = DesktopUpdatePhase.INSTALLING) }
                withContext(Dispatchers.Swing) { onReadyToExit() }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                internalState.value = DesktopUpdateUiState(
                    phase = DesktopUpdatePhase.FAILED,
                    release = release,
                    error = error.message.orEmpty().ifBlank { error::class.simpleName.orEmpty() }
                )
            }
        }
    }

    fun shutdown() {
        checkJob?.cancel()
        installJob?.cancel()
    }

    private fun fetchLatestRelease(): DesktopRelease? {
        val request = Request.Builder()
            .url(DESKTOP_RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Levyra-Desktop/$currentVersion")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub desktop release check failed: HTTP ${response.code}")
            }
            return Json.parseToJsonElement(response.body.string())
                .jsonArray
                .mapNotNull { parseDesktopRelease(it.jsonObject) }
                .reduceOrNull { newest, candidate ->
                    if (DesktopVersion.isNewer(candidate.version, newest.version)) candidate else newest
                }
        }
    }

    private fun parseDesktopRelease(root: JsonObject): DesktopRelease? {
        if (root["draft"]?.jsonPrimitive?.booleanOrNull == true) return null
        if (root["prerelease"]?.jsonPrimitive?.booleanOrNull == true) return null

        val tag = root["tag_name"]
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()
            .trim()
        if (!tag.startsWith(DESKTOP_RELEASE_TAG_PREFIX)) return null

        val version = tag.removePrefix(DESKTOP_RELEASE_TAG_PREFIX).trim()
        if (version.isBlank()) return null

        val assets = root["assets"]?.jsonArray.orEmpty().mapNotNull { element ->
            val asset = element.jsonObject
            val name = asset["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (name.isBlank() || url.isBlank()) {
                null
            } else {
                DesktopReleaseAsset(
                    name = name,
                    url = url,
                    size = asset["size"]?.jsonPrimitive?.longOrNull ?: 0L
                )
            }
        }
        val expectedName = "LEVYRA-Windows-$version-x64.msi"
        val installer = assets.firstOrNull { it.name.equals(expectedName, ignoreCase = true) }
            ?: return null
        val checksum = assets.firstOrNull {
            it.name.equals("${installer.name}.sha256", ignoreCase = true)
        } ?: return null
        return DesktopRelease(
            version = version,
            name = root["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank {
                "Levyra Desktop $version"
            },
            notes = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
            installer = installer,
            checksum = checksum
        )
    }

    private suspend fun downloadInstaller(release: DesktopRelease): Path = withContext(Dispatchers.IO) {
        val updateDirectory = paths.root.resolve(UPDATE_DIRECTORY)
        Files.createDirectories(updateDirectory)
        val fileName = Path.of(release.installer.name).fileName.toString()
        val target = updateDirectory.resolve(fileName)
        val temporary = updateDirectory.resolve("$fileName.part")
        Files.deleteIfExists(temporary)

        val request = Request.Builder()
            .url(release.installer.url)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "Levyra-Desktop/$currentVersion")
            .build()
        val call = client.newCall(request)
        currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        call.execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Download aggiornamento non disponibile: HTTP ${response.code}")
            }
            val body = response.body
            val responseLength = body.contentLength().coerceAtLeast(0L)
            val total = release.installer.size.takeIf { it > 0L } ?: responseLength
            internalState.update { it.copy(totalBytes = total) }
            body.byteStream().buffered(BUFFER_SIZE).use { input ->
                Files.newOutputStream(
                    temporary,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).buffered(BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = 0L
                    var lastPublished = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded - lastPublished >= PROGRESS_INTERVAL_BYTES || downloaded == total) {
                            internalState.update {
                                it.copy(bytesDownloaded = downloaded, totalBytes = total)
                            }
                            lastPublished = downloaded
                        }
                    }
                    output.flush()
                    if (total > 0L && downloaded != total) {
                        throw IllegalStateException("Aggiornamento incompleto: $downloaded di $total byte")
                    }
                    internalState.update {
                        it.copy(
                            bytesDownloaded = downloaded,
                            totalBytes = total.takeIf { value -> value > 0L } ?: downloaded
                        )
                    }
                }
            }
        }

        verifyChecksum(release, temporary)
        moveAtomically(temporary, target)
        target
    }

    private fun verifyChecksum(release: DesktopRelease, installer: Path) {
        try {
            val request = Request.Builder()
                .url(release.checksum.url)
                .header("Accept", "text/plain")
                .header("User-Agent", "Levyra-Desktop/$currentVersion")
                .build()
            val expected = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Checksum aggiornamento non disponibile: HTTP ${response.code}")
                }
                response.body.string().trim().substringBefore(' ').lowercase(Locale.ROOT)
            }
            if (!expected.matches(Regex("^[0-9a-f]{64}$"))) {
                throw IllegalStateException("Checksum aggiornamento non valido")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            Files.newInputStream(installer).buffered(BUFFER_SIZE).use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            if (!actual.equals(expected, ignoreCase = true)) {
                throw IllegalStateException("Verifica SHA-256 dell'aggiornamento non riuscita")
            }
        } catch (error: Throwable) {
            Files.deleteIfExists(installer)
            throw error
        }
    }

    private fun launchInstaller(release: DesktopRelease, installer: Path) {
        val osName = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
        if (!osName.contains("win")) {
            throw IllegalStateException("Aggiornamento automatico disponibile solo su Windows")
        }
        val updateDirectory = installer.parent
        val script = updateDirectory.resolve("install-levyra-update.ps1")
        val installerLog = updateDirectory.resolve("installer-${release.version}.log")
        val updaterLog = updateDirectory.resolve("updater-${release.version}.log")
        Files.writeString(script, updaterScript(), StandardCharsets.UTF_8)
        val launcher = ProcessHandle.current().info().command().orElse("")
        val powershell = windowsPowerShell()
        ProcessBuilder(
            powershell,
            "-NoLogo",
            "-NoProfile",
            "-NonInteractive",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            script.toString(),
            "-ParentPid",
            ProcessHandle.current().pid().toString(),
            "-Installer",
            installer.toString(),
            "-Launcher",
            launcher,
            "-InstallerLog",
            installerLog.toString()
        )
            .directory(updateDirectory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(updaterLog.toFile())
            .start()
    }

    private fun updaterScript(): String = """
param(
    [long]${'$'}ParentPid,
    [string]${'$'}Installer,
    [string]${'$'}Launcher,
    [string]${'$'}InstallerLog
)
${'$'}ErrorActionPreference = 'Stop'
try {
    Wait-Process -Id ${'$'}ParentPid -ErrorAction SilentlyContinue
} catch {
}
${'$'}escapedInstaller = '"' + ${'$'}Installer.Replace('"', '\"') + '"'
${'$'}escapedLog = '"' + ${'$'}InstallerLog.Replace('"', '\"') + '"'
${'$'}arguments = "/i ${'$'}escapedInstaller /passive /norestart /L*v ${'$'}escapedLog"
${'$'}process = Start-Process -FilePath "${'$'}env:SystemRoot\System32\msiexec.exe" -ArgumentList ${'$'}arguments -Wait -PassThru
if (${'$'}process.ExitCode -eq 0 -or ${'$'}process.ExitCode -eq 1641 -or ${'$'}process.ExitCode -eq 3010) {
    if (${'$'}Launcher -and (Test-Path -LiteralPath ${'$'}Launcher)) {
        Start-Process -FilePath ${'$'}Launcher
    }
    exit 0
}
exit ${'$'}process.ExitCode
""".trimIndent()

    private fun windowsPowerShell(): String {
        val systemRoot = System.getenv("SystemRoot").orEmpty()
        if (systemRoot.isNotBlank()) {
            val candidate = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe")
            if (Files.isRegularFile(candidate)) return candidate.toString()
        }
        return "powershell.exe"
    }

    private fun moveAtomically(source: Path, target: Path) {
        runCatching {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.getOrElse {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val DESKTOP_RELEASES_URL =
            "https://api.github.com/repos/LUC4N3X/Levyra-deepsound/releases?per_page=50"
        const val DESKTOP_RELEASE_TAG_PREFIX = "desktop-v"
        const val UPDATE_DIRECTORY = "updates"
        const val BUFFER_SIZE = 64 * 1024
        const val PROGRESS_INTERVAL_BYTES = 256 * 1024L
    }
}

internal object DesktopVersion {
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateVersion = parse(candidate) ?: return false
        val currentVersion = parse(current) ?: return false
        val maxSize = maxOf(candidateVersion.numbers.size, currentVersion.numbers.size)
        repeat(maxSize) { index ->
            val left = candidateVersion.numbers.getOrElse(index) { 0 }
            val right = currentVersion.numbers.getOrElse(index) { 0 }
            if (left != right) return left > right
        }
        if (candidateVersion.prerelease.isEmpty() && currentVersion.prerelease.isNotEmpty()) return true
        if (candidateVersion.prerelease.isNotEmpty() && currentVersion.prerelease.isEmpty()) return false
        val prereleaseSize = maxOf(candidateVersion.prerelease.size, currentVersion.prerelease.size)
        repeat(prereleaseSize) { index ->
            val left = candidateVersion.prerelease.getOrNull(index) ?: return false
            val right = currentVersion.prerelease.getOrNull(index) ?: return true
            if (left == right) return@repeat
            val leftNumber = left.toIntOrNull()
            val rightNumber = right.toIntOrNull()
            return when {
                leftNumber != null && rightNumber != null -> leftNumber > rightNumber
                leftNumber != null -> false
                rightNumber != null -> true
                else -> left > right
            }
        }
        return false
    }

    private fun parse(raw: String): ParsedVersion? {
        val normalized = raw.trim().removePrefix("v").substringBefore('+')
        val core = normalized.substringBefore('-')
        val numbers = core.split('.').map { part ->
            if (part.isBlank() || part.any { !it.isDigit() }) return null
            part.toIntOrNull() ?: return null
        }
        if (numbers.isEmpty()) return null
        val prerelease = normalized.substringAfter('-', "")
            .split('.')
            .filter { it.isNotBlank() }
        return ParsedVersion(numbers = numbers, prerelease = prerelease)
    }

    private data class ParsedVersion(
        val numbers: List<Int>,
        val prerelease: List<String>
    )
}
