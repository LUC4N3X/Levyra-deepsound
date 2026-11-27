package com.luc4n3x.levyra.data.youtubemusic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

internal class YoutubeMusicLinkUnreachable : IOException("No computer answered this code")

internal class YoutubeMusicLinkConnection internal constructor(
    val device: YoutubeMusicLinkDevice,
    private val socket: Socket,
    private val channel: YoutubeMusicLinkChannel
) {
    suspend fun send(payload: YoutubeMusicLinkPayload): YoutubeMusicLinkResult = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = YoutubeMusicDeviceLink.REPLY_TIMEOUT_MS
            val output = socket.getOutputStream()
            val plain = YoutubeMusicDeviceLink.encodePayload(payload)
            try {
                YoutubeMusicDeviceLink.writeMessage(
                    output,
                    YoutubeMusicDeviceLink.message(YoutubeMusicDeviceLink.LOGINS, channel.seal(YoutubeMusicDeviceLink.LOGINS, false, plain))
                )
            } finally {
                plain.fill(0)
            }
            val reply = YoutubeMusicDeviceLink.readMessage(socket.getInputStream())
            YoutubeMusicDeviceLink.decodeResult(
                channel.open(YoutubeMusicDeviceLink.LOGINS, true, YoutubeMusicDeviceLink.sealedOf(reply, YoutubeMusicDeviceLink.LOGINS))
            )
        } finally {
            close()
        }
    }

    internal fun owns(candidate: Socket): Boolean = socket === candidate

    fun cancel() {
        runCatching {
            YoutubeMusicDeviceLink.writeMessage(
                socket.getOutputStream(),
                YoutubeMusicDeviceLink.message(YoutubeMusicDeviceLink.CANCEL, channel.seal(YoutubeMusicDeviceLink.CANCEL, false, "{}".toByteArray()))
            )
        }
        close()
    }

    private fun close() {
        channel.close()
        runCatching { socket.close() }
    }
}

internal object YoutubeMusicLinkClient {
    suspend fun connect(offer: YoutubeMusicLinkOffer): YoutubeMusicLinkConnection = coroutineScope {
        val sockets = offer.hosts.map { Socket() }
        val attempts = offer.hosts.mapIndexed { index, host ->
            async(Dispatchers.IO) { runCatching { hello(offer, host, sockets[index]) } }
        }
        var winner: YoutubeMusicLinkConnection? = null
        try {
            var lastError: Throwable? = null
            val pending = attempts.toMutableList()
            while (pending.isNotEmpty() && winner == null) {
                val (done, result) = select {
                    pending.forEach { attempt -> attempt.onAwait { attempt to it } }
                }
                pending.remove(done)
                winner = result.getOrNull()
                val error = result.exceptionOrNull()
                if (error is YoutubeMusicLinkException || (error != null && lastError == null)) lastError = error
            }
            winner ?: throw lastError as? YoutubeMusicLinkException ?: YoutubeMusicLinkUnreachable()
        } finally {
            sockets.filterNot { winner?.owns(it) == true }.forEach { runCatching { it.close() } }
            attempts.forEach { attempt ->
                if (attempt.isCompleted) attempt.getCompleted().getOrNull()?.takeIf { it !== winner }?.cancel()
            }
        }
    }

    private fun hello(offer: YoutubeMusicLinkOffer, host: String, socket: Socket): YoutubeMusicLinkConnection {
        try {
            socket.connect(InetSocketAddress(host, offer.port), YoutubeMusicDeviceLink.CONNECT_TIMEOUT_MS)
            socket.soTimeout = YoutubeMusicDeviceLink.REPLY_TIMEOUT_MS
            socket.tcpNoDelay = true
            val keys = YoutubeMusicDeviceLink.newHostKeys()
            val channel = YoutubeMusicDeviceLink.channel(keys.private, YoutubeMusicDeviceLink.publicKey(offer.publicKey), offer.session)
            try {
                YoutubeMusicDeviceLink.writeMessage(socket.getOutputStream(), YoutubeMusicDeviceLink.helloRequest(keys.public, channel))
                val reply = YoutubeMusicDeviceLink.readMessage(socket.getInputStream())
                if (reply.optString("t") == STATUS) {
                    throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.of(reply.optInt("s", YoutubeMusicLinkStatus.Closed.code)))
                }
                val device = YoutubeMusicDeviceLink.decodeDevice(
                    channel.open(YoutubeMusicDeviceLink.HELLO, true, YoutubeMusicDeviceLink.sealedOf(reply, YoutubeMusicDeviceLink.HELLO))
                )
                return YoutubeMusicLinkConnection(device, socket, channel)
            } catch (error: Throwable) {
                channel.close()
                throw error
            }
        } catch (error: Throwable) {
            runCatching { socket.close() }
            throw error
        }
    }

    const val STATUS = "status"
}
