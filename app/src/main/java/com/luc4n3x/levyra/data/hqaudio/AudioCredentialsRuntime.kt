package com.luc4n3x.levyra.data.hqaudio

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.buffer
import okio.source

internal object AudioCredentialsRuntime : AudioCredentialsSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshQueued = AtomicBoolean()
    private val transport = AudioCredentialsTransport()
    @Volatile private var store = AudioCredentialsStore(transport::fetch)
    private var initialized = false

    @Synchronized fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        val file by lazy { AtomicFile(File(appContext.noBackupFilesDir, "audio-credentials.json")) }
        store = AudioCredentialsStore(transport::fetch, object : AudioCredentialsCache {
            override fun read(): String = file.openRead().source().buffer().use(AudioCredentialsTransport::readBounded)

            override fun write(document: String) {
                val output = file.startWrite()
                try {
                    output.write(document.toByteArray(Charsets.UTF_8))
                    file.finishWrite(output)
                } catch (error: Exception) {
                    file.failWrite(output)
                    throw error
                }
            }
        })
        initialized = true
        scheduleRefresh()
    }

    override suspend fun current(): AudioCredentials = store.current().also { scheduleRefresh() }

    override suspend fun refreshAfterRejection(rejected: AudioCredentials): AudioCredentials =
        store.refreshAfterRejection(rejected)

    private fun scheduleRefresh() {
        if (!refreshQueued.compareAndSet(false, true)) return
        scope.launch {
            try { store.refresh() } finally { refreshQueued.set(false) }
        }
    }
}
