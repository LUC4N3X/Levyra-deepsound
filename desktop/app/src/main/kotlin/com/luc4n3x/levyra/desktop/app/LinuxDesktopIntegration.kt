package com.luc4n3x.levyra.desktop.app

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

internal object LinuxDesktopIntegration {
    fun register() {
        val launcher = System.getProperty("jpackage.app-path").orEmpty().ifBlank {
            ProcessHandle.current().info().command().orElse("")
        }
        val executable = runCatching { Path.of(launcher) }.getOrNull() ?: return
        if (executable.fileName.toString() != "Levyra" || !Files.isExecutable(executable)) return
        val home = Path.of(System.getProperty("user.home"))
        val dataHome = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(Path::of)
            ?.takeIf { it.isAbsolute } ?: home.resolve(".local/share")
        runCatching {
            val applications = Files.createDirectories(dataHome.resolve("applications"))
            val target = applications.resolve("levyra-url.desktop")
            val content = desktopEntry(executable)
            if (Files.exists(target) && Files.readString(target) == content) return
            val temporary = Files.createTempFile(applications, ".levyra-", ".desktop")
            try {
                Files.writeString(temporary, content)
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } finally { Files.deleteIfExists(temporary) }
            val process = ProcessBuilder("xdg-mime", "default", "levyra-url.desktop", "x-scheme-handler/levyra")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly()
        }.onFailure { System.getLogger("Levyra.Desktop").log(System.Logger.Level.DEBUG, "Linux protocol registration unavailable", it) }
    }

    fun desktopEntry(executable: Path): String {
        val command = executable.toAbsolutePath().toString()
        require(command.none { it == '\n' || it == '\r' })
        val quoted = command.replace("\\", "\\\\\\\\").replace("\"", "\\\"")
            .replace("$", "\\$").replace("`", "\\`").replace("%", "%%")
        return "[Desktop Entry]\nType=Application\nName=Levyra\nExec=\"$quoted\" %u\nTerminal=false\nNoDisplay=true\nMimeType=x-scheme-handler/levyra;\n"
    }
}
