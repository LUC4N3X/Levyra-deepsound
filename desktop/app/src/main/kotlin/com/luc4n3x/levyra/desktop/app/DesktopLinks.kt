package com.luc4n3x.levyra.desktop.app

import com.luc4n3x.levyra.desktop.app.state.Destination
import com.luc4n3x.levyra.desktop.app.state.LevyraAppModel
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

internal object DesktopProtocolRegistrar {
    private const val SCHEME = "levyra"
    private const val REGISTRY_KEY = "HKCU\\Software\\Classes\\$SCHEME"

    fun register() {
        if (!isWindows()) return
        val executable = resolveExecutable() ?: return
        val current = queryDefault("$REGISTRY_KEY\\shell\\open\\command")
        if (current?.contains(executable, ignoreCase = true) == true) return

        addDefault(REGISTRY_KEY, "URL:Levyra Protocol")
        addValue(REGISTRY_KEY, "URL Protocol", "")
        addDefault("$REGISTRY_KEY\\DefaultIcon", "\"$executable\",0")
        addDefault("$REGISTRY_KEY\\shell\\open\\command", "\"$executable\" \"%1\"")
    }

    private fun resolveExecutable(): String? {
        val command = ProcessHandle.current().info().command().orElse("")
        if (command.endsWith(".exe", ignoreCase = true) && Files.isRegularFile(Path.of(command))) {
            return command
        }

        val javaHome = System.getProperty("java.home").orEmpty()
        if (javaHome.isBlank()) return null
        val runtime = generateSequence(Path.of(javaHome)) { it.parent }
            .firstOrNull { it.fileName?.toString().equals("runtime", ignoreCase = true) }
        val appDirectory = runtime?.parent ?: return null
        val executable = appDirectory.resolve("Levyra.exe")
        return executable.takeIf(Files::isRegularFile)?.toString()
    }

    private fun addDefault(key: String, data: String) {
        runRegistry("reg add \"$key\" /f /ve /t REG_SZ /d \"${escape(data)}\"")
    }

    private fun addValue(key: String, name: String, data: String) {
        runRegistry("reg add \"$key\" /f /v \"$name\" /t REG_SZ /d \"${escape(data)}\"")
    }

    private fun queryDefault(key: String): String? {
        return runCatching {
            val process = ProcessBuilder("reg", "query", key, "/ve")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() == 0) output else null
        }.getOrNull()
    }

    private fun runRegistry(command: String) {
        runCatching {
            ProcessBuilder("cmd.exe", "/c", command)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        }
    }

    private fun escape(value: String): String = value.replace("\"", "\\\"")

    private fun isWindows(): Boolean = System.getProperty("os.name")
        .orEmpty()
        .lowercase(Locale.ROOT)
        .contains("win")
}

internal object DesktopLinkRouter {
    fun launchPayload(args: Array<String>): String = args.firstOrNull { argument ->
        argument.startsWith("levyra://", ignoreCase = true) ||
            argument.startsWith("https://", ignoreCase = true) ||
            argument.startsWith("http://", ignoreCase = true)
    }.orEmpty()

    fun route(raw: String, model: LevyraAppModel) {
        val action = parse(raw) ?: return
        when (action) {
            is DesktopLaunchAction.OpenUrl -> model.openCollectionFromUrl(action.url)
            is DesktopLaunchAction.Search -> {
                model.catalogController.clearSearch()
                model.catalogController.onQueryChange(action.query)
                model.catalogController.submit(action.query)
                model.navigate(Destination.SEARCH)
            }
            DesktopLaunchAction.ShowHome -> model.navigate(Destination.HOME)
        }
    }

    private fun parse(raw: String): DesktopLaunchAction? {
        val clean = raw.trim()
        if (clean.isBlank()) return DesktopLaunchAction.ShowHome
        val uri = runCatching { URI(clean) }.getOrNull() ?: return null
        val scheme = uri.scheme.orEmpty().lowercase(Locale.ROOT)

        if (scheme == "levyra") {
            val host = uri.host.orEmpty().lowercase(Locale.ROOT)
            val query = queryParameters(uri)
            return when (host) {
                "open", "open-app" -> query["url"]
                    ?.takeIf { it.isNotBlank() }
                    ?.let(DesktopLaunchAction::OpenUrl)
                    ?: DesktopLaunchAction.ShowHome
                "search" -> query["q"]
                    ?.takeIf { it.isNotBlank() }
                    ?.let(DesktopLaunchAction::Search)
                    ?: DesktopLaunchAction.ShowHome
                "watch", "playlist", "channel", "album", "browse" -> {
                    val path = uri.rawPath.orEmpty()
                    val rawQuery = uri.rawQuery?.let { "?$it" }.orEmpty()
                    DesktopLaunchAction.OpenUrl("https://music.youtube.com/$host$path$rawQuery")
                }
                else -> DesktopLaunchAction.ShowHome
            }
        }

        if (scheme == "http" || scheme == "https") {
            val host = uri.host.orEmpty().lowercase(Locale.ROOT)
            val youtubeHost = host == "youtu.be" ||
                host.endsWith("youtube.com") ||
                host.endsWith("music.youtube.com")
            return if (youtubeHost) DesktopLaunchAction.OpenUrl(clean) else null
        }

        return null
    }

    private fun queryParameters(uri: URI): Map<String, String> {
        return uri.rawQuery
            .orEmpty()
            .split('&')
            .mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null
                val key = part.substringBefore('=')
                val value = part.substringAfter('=', "")
                decode(key) to decode(value)
            }
            .toMap()
    }

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)
}

private sealed interface DesktopLaunchAction {
    data class OpenUrl(val url: String) : DesktopLaunchAction
    data class Search(val query: String) : DesktopLaunchAction
    data object ShowHome : DesktopLaunchAction
}
