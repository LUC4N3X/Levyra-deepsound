package com.luc4n3x.levyra.desktop.core.storage

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class JsonFileStore<T>(
    private val file: Path,
    private val serializer: KSerializer<T>,
    private val defaultValue: () -> T,
    private val json: Json = DEFAULT_JSON
) {

    fun read(): T {
        if (!Files.isRegularFile(file)) return defaultValue()
        val raw = runCatching { Files.readString(file, StandardCharsets.UTF_8) }.getOrNull()
        if (raw.isNullOrBlank()) return defaultValue()
        return runCatching { json.decodeFromString(serializer, raw) }
            .getOrElse {
                quarantine()
                defaultValue()
            }
    }

    @Synchronized
    fun write(value: T) {
        val parent = file.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        val payload = json.encodeToString(serializer, value)
        val temporary = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temporary, payload, StandardCharsets.UTF_8)
        runCatching {
            Files.move(
                temporary,
                file,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.onFailure {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun quarantine() {
        val broken = file.resolveSibling("${file.fileName}.invalid")
        runCatching { Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING) }
    }

    companion object {
        val DEFAULT_JSON: Json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
