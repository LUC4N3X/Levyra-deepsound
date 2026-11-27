package com.luc4n3x.levyra.desktop.core.account

import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAuth
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.DriverManager
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class BrowserKind(val label: String, val chromium: Boolean) {
    Firefox("Firefox", false),
    LibreWolf("LibreWolf", false),
    Chrome("Google Chrome", true),
    Chromium("Chromium", true),
    Edge("Microsoft Edge", true),
    Brave("Brave", true),
    Vivaldi("Vivaldi", true),
    Opera("Opera", true)
}

enum class BrowserProfileStatus { Ready, ProtectedOnWindows }

enum class BrowserImportFailure { NotSignedIn, Locked, KeyUnavailable, Unreadable }

class BrowserImportException(val failure: BrowserImportFailure) : IOException("Browser sign-in import failed: $failure")

data class BrowserProfile(
    val id: String,
    val kind: BrowserKind,
    val name: String,
    val status: BrowserProfileStatus,
    internal val directory: Path
)

internal data class BrowserCookie(val host: String, val name: String, val value: String, val path: String, val expiresAtSeconds: Long)

class BrowserCookieImport internal constructor(
    private val home: Path,
    private val environment: (String) -> String?,
    private val windows: Boolean,
    private val keyringPassword: (BrowserKind) -> ByteArray?
) {
    constructor() : this(
        Path.of(System.getProperty("user.home").orEmpty()),
        System::getenv,
        System.getProperty("os.name").orEmpty().startsWith("Windows"),
        ::chromiumKeyringPassword
    )

    suspend fun profiles(): List<BrowserProfile> = withContext(Dispatchers.IO) {
        (geckoProfiles() + chromiumProfiles()).distinctBy { it.id }
    }

    suspend fun cookieHeader(profile: BrowserProfile): String = withContext(Dispatchers.IO) {
        if (profile.status != BrowserProfileStatus.Ready) throw BrowserImportException(BrowserImportFailure.KeyUnavailable)
        val cookies = if (profile.kind.chromium) chromiumCookies(profile) else geckoCookies(profile.directory)
        val header = youtubeCookieHeader(cookies, System.currentTimeMillis() / 1_000)
        if (runCatching { YoutubeMusicAuth.authorization(header, 1) }.isFailure) throw BrowserImportException(BrowserImportFailure.NotSignedIn)
        header
    }

    private fun geckoRoots(): List<Pair<BrowserKind, Path>> = if (windows) {
        val appData = environment("APPDATA")?.let(Path::of) ?: home.resolve("AppData").resolve("Roaming")
        listOf(BrowserKind.Firefox to appData.resolve("Mozilla").resolve("Firefox"), BrowserKind.LibreWolf to appData.resolve("librewolf"))
    } else {
        listOf(
            BrowserKind.Firefox to home.resolve(".mozilla/firefox"),
            BrowserKind.Firefox to home.resolve("snap/firefox/common/.mozilla/firefox"),
            BrowserKind.Firefox to home.resolve(".var/app/org.mozilla.firefox/.mozilla/firefox"),
            BrowserKind.LibreWolf to home.resolve(".librewolf"),
            BrowserKind.LibreWolf to home.resolve(".var/app/io.gitlab.librewolf-community/.librewolf")
        )
    }

    private fun geckoProfiles(): List<BrowserProfile> = geckoRoots().flatMap { (kind, root) ->
        val ini = root.resolve("profiles.ini")
        if (!Files.isRegularFile(ini)) return@flatMap emptyList()
        parseProfilesIni(runCatching { Files.readAllLines(ini) }.getOrDefault(emptyList())).mapNotNull { (name, relative, path) ->
            val directory = (if (relative) root.resolve(path) else Path.of(path)).normalize()
            if (!Files.isRegularFile(directory.resolve("cookies.sqlite"))) return@mapNotNull null
            BrowserProfile("${kind.name}:$directory", kind, name, BrowserProfileStatus.Ready, directory)
        }
    }

    private fun chromiumRoots(): List<Pair<BrowserKind, Path>> = if (windows) {
        val local = environment("LOCALAPPDATA")?.let(Path::of) ?: home.resolve("AppData").resolve("Local")
        val roaming = environment("APPDATA")?.let(Path::of) ?: home.resolve("AppData").resolve("Roaming")
        listOf(
            BrowserKind.Chrome to local.resolve("Google/Chrome/User Data"),
            BrowserKind.Chromium to local.resolve("Chromium/User Data"),
            BrowserKind.Edge to local.resolve("Microsoft/Edge/User Data"),
            BrowserKind.Brave to local.resolve("BraveSoftware/Brave-Browser/User Data"),
            BrowserKind.Vivaldi to local.resolve("Vivaldi/User Data"),
            BrowserKind.Opera to roaming.resolve("Opera Software/Opera Stable")
        )
    } else {
        val config = environment("XDG_CONFIG_HOME")?.takeIf(String::isNotBlank)?.let(Path::of) ?: home.resolve(".config")
        listOf(
            BrowserKind.Chrome to config.resolve("google-chrome"),
            BrowserKind.Chromium to config.resolve("chromium"),
            BrowserKind.Chromium to home.resolve("snap/chromium/common/chromium"),
            BrowserKind.Edge to config.resolve("microsoft-edge"),
            BrowserKind.Brave to config.resolve("BraveSoftware/Brave-Browser"),
            BrowserKind.Vivaldi to config.resolve("vivaldi"),
            BrowserKind.Opera to config.resolve("opera")
        )
    }

    private fun chromiumProfiles(): List<BrowserProfile> = chromiumRoots().flatMap { (kind, root) ->
        if (!Files.isDirectory(root)) return@flatMap emptyList()
        val names = chromiumProfileNames(root)
        val candidates = runCatching {
            Files.list(root).use { stream -> stream.filter { Files.isDirectory(it) }.toList() }
        }.getOrDefault(emptyList()) + root
        candidates.mapNotNull { directory ->
            if (chromiumCookieFile(directory) == null) return@mapNotNull null
            val folder = directory.fileName?.toString().orEmpty()
            val name = names[folder] ?: if (directory == root) kind.label else folder
            val status = if (windows) BrowserProfileStatus.ProtectedOnWindows else BrowserProfileStatus.Ready
            BrowserProfile("${kind.name}:$directory", kind, name, status, directory)
        }
    }

    private fun chromiumProfileNames(root: Path): Map<String, String> = runCatching {
        val cache = JSONObject(Files.readString(root.resolve("Local State"))).optJSONObject("profile")?.optJSONObject("info_cache")
            ?: return@runCatching emptyMap()
        cache.keySet().associateWith { cache.optJSONObject(it)?.optString("name").orEmpty().ifBlank { it } }
    }.getOrDefault(emptyMap())

    private fun geckoCookies(directory: Path): List<BrowserCookie> = withCopiedDatabase(directory.resolve("cookies.sqlite")) { url ->
        DriverManager.getConnection(url, readOnly()).use { connection ->
            val columns = connection.metaData.getColumns(null, null, "moz_cookies", "originAttributes").use { it.next() }
            val sql = "SELECT host, name, value, path, expiry FROM moz_cookies WHERE (host LIKE '%youtube.com')" +
                if (columns) " AND originAttributes = ''" else ""
            connection.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { rows ->
                    buildList {
                        while (rows.next()) {
                            val expiry = rows.getLong(5)
                            add(BrowserCookie(rows.getString(1), rows.getString(2), rows.getString(3).orEmpty(), rows.getString(4).orEmpty(),
                                if (expiry > MILLISECOND_EXPIRY_THRESHOLD) expiry / 1_000 else expiry))
                        }
                    }
                }
            }
        }
    }

    private fun chromiumCookies(profile: BrowserProfile): List<BrowserCookie> {
        val file = chromiumCookieFile(profile.directory) ?: throw BrowserImportException(BrowserImportFailure.Unreadable)
        val password by lazy { keyringPassword(profile.kind) }
        return withCopiedDatabase(file) { url ->
            DriverManager.getConnection(url, readOnly()).use { connection ->
                val version = runCatching {
                    connection.prepareStatement("SELECT value FROM meta WHERE key = 'version'").use { statement ->
                        statement.executeQuery().use { if (it.next()) it.getString(1).toInt() else 0 }
                    }
                }.getOrDefault(0)
                connection.prepareStatement(
                    "SELECT host_key, name, value, encrypted_value, path, expires_utc FROM cookies WHERE host_key LIKE '%youtube.com'"
                ).use { statement ->
                    statement.executeQuery().use { rows ->
                        buildList {
                            while (rows.next()) {
                                val host = rows.getString(1)
                                val encrypted = rows.getBytes(4) ?: ByteArray(0)
                                val value = if (encrypted.isEmpty()) rows.getString(3).orEmpty()
                                else decryptChromium(encrypted, host, version) { password }
                                add(BrowserCookie(host, rows.getString(2), value, rows.getString(5).orEmpty(), chromiumExpiry(rows.getLong(6))))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun chromiumCookieFile(directory: Path): Path? =
        listOf(directory.resolve("Network/Cookies"), directory.resolve("Cookies")).firstOrNull { Files.isRegularFile(it) }

    private fun <T> withCopiedDatabase(file: Path, block: (String) -> T): T {
        val temp = Files.createTempDirectory("levyra-cookies")
        try {
            val copy = temp.resolve("cookies.db")
            try {
                Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING)
                listOf("-wal", "-shm").forEach { suffix ->
                    val side = file.resolveSibling(file.fileName.toString() + suffix)
                    if (Files.isRegularFile(side)) Files.copy(side, temp.resolve("cookies.db$suffix"), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (error: IOException) {
                throw BrowserImportException(BrowserImportFailure.Locked)
            }
            return try {
                block("jdbc:sqlite:${copy.toAbsolutePath()}")
            } catch (error: BrowserImportException) {
                throw error
            } catch (error: java.sql.SQLException) {
                throw BrowserImportException(BrowserImportFailure.Unreadable)
            }
        } finally {
            runCatching { Files.walk(temp).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) } }
        }
    }

    private fun readOnly() = Properties().apply { setProperty("open_mode", "1") }

    companion object {
        private const val MILLISECOND_EXPIRY_THRESHOLD = 100_000_000_000L
    }
}

internal fun parseProfilesIni(lines: List<String>): List<Triple<String, Boolean, String>> {
    val profiles = mutableListOf<Triple<String, Boolean, String>>()
    var section = ""
    val values = mutableMapOf<String, String>()
    fun flush() {
        if (section.startsWith("Profile")) {
            val path = values["Path"]
            if (!path.isNullOrBlank()) profiles += Triple(values["Name"].orEmpty().ifBlank { path }, values["IsRelative"] != "0", path)
        }
        values.clear()
    }
    lines.forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("[") && line.endsWith("]") -> {
                flush()
                section = line.substring(1, line.length - 1)
            }
            '=' in line -> values[line.substringBefore('=').trim()] = line.substringAfter('=').trim()
        }
    }
    flush()
    return profiles
}

internal fun youtubeCookieHeader(cookies: List<BrowserCookie>, nowSeconds: Long): String =
    cookies.asSequence()
        .filter { it.host == "music.youtube.com" || it.host == ".music.youtube.com" || it.host == ".youtube.com" || it.host == "youtube.com" }
        .filter { it.path.isEmpty() || it.path == "/" }
        .filter { it.expiresAtSeconds == 0L || it.expiresAtSeconds > nowSeconds }
        .filter { it.name.isNotBlank() && it.value.isNotEmpty() && it.name.none { c -> c == ';' || c == '=' || c.isWhitespace() } && ';' !in it.value }
        .sortedBy { if (it.host.trimStart('.') == "music.youtube.com") 0 else 1 }
        .distinctBy { it.name }
        .joinToString("; ") { "${it.name}=${it.value}" }

internal fun chromiumExpiry(expiresUtc: Long): Long =
    if (expiresUtc <= 0L) 0L else expiresUtc / 1_000_000L - CHROMIUM_EPOCH_OFFSET_SECONDS

private const val CHROMIUM_EPOCH_OFFSET_SECONDS = 11_644_473_600L

internal fun decryptChromium(encrypted: ByteArray, host: String, databaseVersion: Int, password: () -> ByteArray?): String {
    if (encrypted.size < 3) throw BrowserImportException(BrowserImportFailure.Unreadable)
    val prefix = String(encrypted, 0, 3, Charsets.US_ASCII)
    val candidates = when (prefix) {
        "v10" -> listOf("peanuts".toByteArray(Charsets.US_ASCII))
        "v11" -> listOfNotNull(password(), ByteArray(0))
        else -> throw BrowserImportException(BrowserImportFailure.KeyUnavailable)
    }
    candidates.forEach { secret ->
        val plain = runCatching {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                .generateSecret(PBEKeySpec(String(secret, Charsets.UTF_8).toCharArray(), "saltysalt".toByteArray(Charsets.US_ASCII), 1, 128)).encoded
            Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ByteArray(16) { ' '.code.toByte() }))
                doFinal(encrypted, 3, encrypted.size - 3)
            }
        }.getOrNull() ?: return@forEach
        val value = if (databaseVersion >= 24 && plain.size >= 32) plain.copyOfRange(32, plain.size) else plain
        return String(value, Charsets.UTF_8)
    }
    throw BrowserImportException(if (prefix == "v11") BrowserImportFailure.KeyUnavailable else BrowserImportFailure.Unreadable)
}

internal fun chromiumKeyringPassword(kind: BrowserKind): ByteArray? {
    val application = when (kind) {
        BrowserKind.Chrome -> "chrome"
        BrowserKind.Chromium -> "chromium"
        BrowserKind.Edge -> "microsoft-edge"
        BrowserKind.Brave -> "brave"
        BrowserKind.Vivaldi -> "vivaldi"
        BrowserKind.Opera -> "opera"
        else -> return null
    }
    val tool = secretToolPath() ?: return null
    return runSecretTool(listOf(tool, "lookup", "application", application))
        ?.let { output -> String(output, Charsets.UTF_8).trim().takeIf(String::isNotEmpty)?.toByteArray(Charsets.UTF_8) }
}
