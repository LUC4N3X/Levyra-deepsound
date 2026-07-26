package com.luc4n3x.levyra.desktop.core.storage

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloadStore(
    private val store: JsonFileStore<DownloadData>,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val state = MutableStateFlow(store.read())

    val downloads: StateFlow<DownloadData> = state.asStateFlow()
    val current: DownloadData get() = state.value

    fun record(id: String): DownloadRecord? = state.value.records.firstOrNull { it.id == id }

    fun upsert(record: DownloadRecord) = mutate { data ->
        val updated = record.copy(updatedAt = nowMillis())
        val index = data.records.indexOfFirst { it.id == record.id }
        if (index < 0) {
            data.copy(records = listOf(updated) + data.records)
        } else {
            val records = data.records.toMutableList().apply { set(index, updated) }
            data.copy(records = records)
        }
    }

    fun update(id: String, transform: (DownloadRecord) -> DownloadRecord) = mutate { data ->
        val index = data.records.indexOfFirst { it.id == id }
        if (index < 0) return@mutate data
        val records = data.records.toMutableList()
        records[index] = transform(records[index]).copy(updatedAt = nowMillis())
        data.copy(records = records)
    }

    fun remove(id: String) = mutate { data ->
        data.copy(records = data.records.filterNot { it.id == id })
    }

    fun reconcile(downloadsDirectory: Path) = mutate { data ->
        val reconciled = data.records.map { record ->
            when {
                record.status == DownloadStatus.COMPLETED && !isRegularFile(record.filePath) -> {
                    record.copy(
                        status = DownloadStatus.FAILED,
                        error = "File offline non disponibile",
                        filePath = "",
                        bytesDownloaded = existingSize(record.temporaryPath),
                        updatedAt = nowMillis()
                    )
                }

                record.isActive -> {
                    record.copy(
                        status = DownloadStatus.QUEUED,
                        bytesDownloaded = existingSize(record.temporaryPath),
                        error = "",
                        updatedAt = nowMillis()
                    )
                }

                else -> record
            }
        }
        Files.createDirectories(downloadsDirectory)
        data.copy(records = reconciled)
    }

    @Synchronized
    private fun mutate(transform: (DownloadData) -> DownloadData) {
        val updated = transform(state.value)
        if (updated == state.value) return
        store.write(updated)
        state.value = updated
    }

    private fun isRegularFile(path: String): Boolean = path.isNotBlank() && runCatching {
        Files.isRegularFile(Path.of(path))
    }.getOrDefault(false)

    private fun existingSize(path: String): Long = if (path.isBlank()) {
        0L
    } else {
        runCatching { Files.size(Path.of(path)) }.getOrDefault(0L)
    }

    companion object {
        fun create(paths: AppPaths): DownloadStore = DownloadStore(
            JsonFileStore(
                file = paths.downloadsFile,
                serializer = DownloadData.serializer(),
                defaultValue = { DownloadData() }
            )
        )
    }
}
