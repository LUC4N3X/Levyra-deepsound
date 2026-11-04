package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.playlistexport.RemotePlaylistExportProgress
import com.luc4n3x.levyra.data.playlistexport.RemotePlaylistExporter

internal class YoutubeMusicPlaylistExporter(
    private val session: YoutubeMusicSession,
    private val client: YoutubeMusicAccountClient = YoutubeMusicAccountClient()
) : RemotePlaylistExporter {
    override suspend fun export(title: String, videoIds: List<String>, progress: RemotePlaylistExportProgress) {
        val playlistId = client.createPlaylist(session, title.take(MAX_TITLE_LENGTH))
        progress.onCreated("${YoutubeMusicAuth.ORIGIN}/playlist?list=$playlistId")
        var added = 0
        videoIds.chunked(BATCH_SIZE).forEach { batch ->
            client.addToPlaylist(session, playlistId, batch)
            added += batch.size
            progress.onAdded(added)
        }
    }

    private companion object {
        const val BATCH_SIZE = 50
        const val MAX_TITLE_LENGTH = 150
    }
}
