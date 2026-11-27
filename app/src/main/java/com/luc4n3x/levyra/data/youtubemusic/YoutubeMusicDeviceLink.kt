package com.luc4n3x.levyra.data.youtubemusic

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal enum class YoutubeMusicLinkStatus(val code: Int) {
    Accepted(0),
    Rejected(1),
    SignInFailed(2),
    Closed(3);

    companion object {
        fun of(code: Int): YoutubeMusicLinkStatus = entries.firstOrNull { it.code == code } ?: Rejected
    }
}

internal class YoutubeMusicLinkException(val status: YoutubeMusicLinkStatus) : IOException("YouTube Music link failed: $status")

internal class YoutubeMusicLinkPayload(val cookie: String, val accountId: String) {
    override fun toString(): String = "YoutubeMusicLinkPayload(<redacted>)"
}

internal data class YoutubeMusicLinkDevice(val name: String, val os: String)

internal data class YoutubeMusicLinkResult(val status: YoutubeMusicLinkStatus, val accounts: List<String>)

internal class YoutubeMusicLinkOffer(
    val hosts: List<String>,
    val port: Int,
    val publicKey: ByteArray,
    val session: ByteArray
) {
    fun uri(): String = "$PREFIX?v=$VERSION&h=${hosts.joinToString(",")}&p=$port" +
        "&k=${encoder.encodeToString(YoutubeMusicDeviceLink.rawPoint(publicKey))}&s=${encoder.encodeToString(session)}"

    companion object {
        fun parse(value: String): YoutubeMusicLinkOffer? = runCatching {
            val text = value.trim()
            if (text.length > MAX_URI_LENGTH || !text.startsWith("$PREFIX?")) return null
            val query = text.substringAfter('?').split('&').associate {
                it.substringBefore('=') to URLDecoder.decode(it.substringAfter('=', ""), Charsets.UTF_8.name())
            }
            if (query["v"] != VERSION.toString()) return null
            val hosts = query["h"].orEmpty().split(',').map(String::trim).filter(String::isNotBlank).distinct()
            if (hosts.isEmpty() || hosts.size > MAX_HOSTS || !hosts.all(YoutubeMusicDeviceLink::isUsableIpv4)) return null
            val port = query["p"]?.toIntOrNull()?.takeIf { it in 1024..65535 } ?: return null
            val point = decoder.decode(query["k"].orEmpty())
            val session = decoder.decode(query["s"].orEmpty())
            if (session.size != SESSION_BYTES || point.size != RAW_POINT_BYTES) return null
            YoutubeMusicLinkOffer(hosts, port, YoutubeMusicDeviceLink.fromRawPoint(point).encoded, session)
        }.getOrNull()
    }
}

internal class YoutubeMusicLinkChannel(private val key: ByteArray) {
    fun seal(kind: String, reply: Boolean, plain: ByteArray): String {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val sealed = Cipher.getInstance(CIPHER).run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(aad(kind, reply))
            doFinal(plain)
        }
        return encoder.encodeToString(nonce + sealed)
    }

    fun open(kind: String, reply: Boolean, sealed: String): ByteArray {
        val bytes = runCatching { decoder.decode(sealed) }.getOrNull()
        if (bytes == null || bytes.size < NONCE_BYTES + TAG_BITS / 8) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        return try {
            Cipher.getInstance(CIPHER).run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, bytes, 0, NONCE_BYTES))
                updateAAD(aad(kind, reply))
                doFinal(bytes, NONCE_BYTES, bytes.size - NONCE_BYTES)
            }
        } catch (_: GeneralSecurityException) {
            throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        }
    }

    fun close() = key.fill(0)

    private fun aad(kind: String, reply: Boolean): ByteArray =
        "levyra-ytm-link/v$VERSION/$kind/${if (reply) "reply" else "request"}".toByteArray(Charsets.US_ASCII)
}

internal object YoutubeMusicDeviceLink {
    const val HELLO = "hello"
    const val LOGINS = "logins"
    const val CANCEL = "cancel"
    const val CONNECT_TIMEOUT_MS = 4_000
    const val REPLY_TIMEOUT_MS = 30_000

    fun newHostKeys(): KeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec(CURVE), random)
    }.generateKeyPair()

    fun newSession(): ByteArray = ByteArray(SESSION_BYTES).also(random::nextBytes)

    fun publicKey(encoded: ByteArray): PublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(encoded))

    fun rawPoint(encoded: ByteArray): ByteArray {
        val point = (publicKey(encoded) as ECPublicKey).w
        return byteArrayOf(4) + fixed(point.affineX) + fixed(point.affineY)
    }

    fun fromRawPoint(raw: ByteArray): PublicKey {
        require(raw.size == RAW_POINT_BYTES && raw[0] == 4.toByte())
        val point = ECPoint(BigInteger(1, raw.copyOfRange(1, 33)), BigInteger(1, raw.copyOfRange(33, 65)))
        return KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, curve))
    }

    private val curve: ECParameterSpec by lazy {
        AlgorithmParameters.getInstance("EC").apply { init(ECGenParameterSpec(CURVE)) }.getParameterSpec(ECParameterSpec::class.java)
    }

    private fun fixed(value: BigInteger): ByteArray {
        val bytes = value.toByteArray().let { if (it.size > COORDINATE_BYTES) it.copyOfRange(it.size - COORDINATE_BYTES, it.size) else it }
        return ByteArray(COORDINATE_BYTES - bytes.size) + bytes
    }

    fun channel(ownKey: PrivateKey, peer: PublicKey, session: ByteArray): YoutubeMusicLinkChannel {
        val secret = KeyAgreement.getInstance("ECDH").run {
            init(ownKey)
            doPhase(peer, true)
            generateSecret()
        }
        try {
            val prk = hmac(session, secret)
            try {
                return YoutubeMusicLinkChannel(hmac(prk, INFO + byteArrayOf(1)).copyOf(KEY_BYTES))
            } finally {
                prk.fill(0)
            }
        } finally {
            secret.fill(0)
        }
    }

    fun helloRequest(phoneKey: PublicKey, channel: YoutubeMusicLinkChannel): JSONObject = JSONObject()
        .put("t", HELLO)
        .put("k", encoder.encodeToString(phoneKey.encoded))
        .put("d", channel.seal(HELLO, false, EMPTY_OBJECT))

    fun phoneKeyOf(hello: JSONObject): PublicKey = try {
        if (hello.optString("t") != HELLO) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        val encoded = decoder.decode(hello.getString("k"))
        if (encoded.size > MAX_PUBLIC_KEY_BYTES) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        publicKey(encoded)
    } catch (error: YoutubeMusicLinkException) {
        throw error
    } catch (_: Exception) {
        throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
    }

    fun encodeDevice(device: YoutubeMusicLinkDevice): ByteArray =
        JSONObject().put("name", device.name).put("os", device.os).toString().toByteArray(Charsets.UTF_8)

    fun decodeDevice(raw: ByteArray): YoutubeMusicLinkDevice = parse(raw) { root ->
        YoutubeMusicLinkDevice(
            root.optString("name").filterNot(Char::isISOControl).take(MAX_LABEL).trim(),
            root.optString("os").filterNot(Char::isISOControl).take(MAX_LABEL).trim()
        )
    }

    fun encodePayload(payload: YoutubeMusicLinkPayload): ByteArray =
        JSONObject().put("cookie", payload.cookie).put("account", payload.accountId).toString().toByteArray(Charsets.UTF_8)

    fun decodePayload(raw: ByteArray): YoutubeMusicLinkPayload = parse(raw) { root ->
        val cookie = root.optString("cookie")
        if (cookie.isBlank() || cookie.length > MAX_COOKIE_LENGTH) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        YoutubeMusicLinkPayload(cookie, root.optString("account").take(MAX_LABEL))
    }

    fun encodeResult(result: YoutubeMusicLinkResult): ByteArray = JSONObject()
        .put("status", result.status.code)
        .put("accounts", JSONArray(result.accounts.take(MAX_ACCOUNTS)))
        .toString().toByteArray(Charsets.UTF_8)

    fun decodeResult(raw: ByteArray): YoutubeMusicLinkResult = parse(raw) { root ->
        val names = root.optJSONArray("accounts")
        YoutubeMusicLinkResult(
            YoutubeMusicLinkStatus.of(root.optInt("status", YoutubeMusicLinkStatus.Rejected.code)),
            (0 until minOf(names?.length() ?: 0, MAX_ACCOUNTS)).map { names!!.optString(it).take(MAX_LABEL) }.filter(String::isNotBlank)
        )
    }

    fun message(kind: String, sealed: String): JSONObject = JSONObject().put("t", kind).put("d", sealed)

    fun writeMessage(output: OutputStream, message: JSONObject) {
        val body = message.toString().toByteArray(Charsets.UTF_8)
        if (body.size > MAX_FRAME_BYTES) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        val data = DataOutputStream(output)
        data.write(MAGIC)
        data.writeInt(body.size)
        data.write(body)
        data.flush()
    }

    fun readMessage(input: InputStream): JSONObject {
        val data = DataInputStream(input)
        val magic = ByteArray(MAGIC.size)
        data.readFully(magic)
        if (!magic.contentEquals(MAGIC)) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        val size = data.readInt()
        if (size !in 1..MAX_FRAME_BYTES) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        val body = ByteArray(size).also(data::readFully)
        return try {
            JSONObject(String(body, Charsets.UTF_8))
        } catch (_: JSONException) {
            throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        }
    }

    fun sealedOf(message: JSONObject, kind: String): String {
        if (message.optString("t") != kind) throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
        return message.optString("d").takeIf(String::isNotBlank) ?: throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
    }

    fun isUsableIpv4(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { part ->
            part.takeIf { it.length in 1..3 && it.all(Char::isDigit) }?.toInt()?.takeIf { it in 0..255 } ?: return false
        }
        return octets[0] !in UNUSABLE_FIRST_OCTETS && octets[0] < 224 && !(octets[0] == 169 && octets[1] == 254)
    }

    private inline fun <T> parse(raw: ByteArray, block: (JSONObject) -> T): T = try {
        block(JSONObject(String(raw, Charsets.UTF_8)))
    } catch (_: JSONException) {
        throw YoutubeMusicLinkException(YoutubeMusicLinkStatus.Rejected)
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(data)
    }

    private val UNUSABLE_FIRST_OCTETS = setOf(0, 127)
}

private const val PREFIX = "levyra://ytm-link"
private const val VERSION = 1
private const val CURVE = "secp256r1"
private const val CIPHER = "AES/GCM/NoPadding"
private const val SESSION_BYTES = 16
private const val RAW_POINT_BYTES = 65
private const val COORDINATE_BYTES = 32
private const val NONCE_BYTES = 12
private const val KEY_BYTES = 32
private const val TAG_BITS = 128
private const val MAX_HOSTS = 6
private const val MAX_URI_LENGTH = 1_024
private const val MAX_PUBLIC_KEY_BYTES = 160
private const val MAX_LABEL = 64
private const val MAX_ACCOUNTS = 20
private const val MAX_FRAME_BYTES = 64 * 1024
private const val MAX_COOKIE_LENGTH = 16 * 1024
private val EMPTY_OBJECT = "{}".toByteArray(Charsets.US_ASCII)
private val MAGIC = byteArrayOf('L'.code.toByte(), 'V'.code.toByte(), 'Y'.code.toByte(), 'L'.code.toByte())
private val INFO = "levyra-ytm-link-v1".toByteArray(Charsets.US_ASCII)
private val random = SecureRandom()
private val encoder = Base64.getUrlEncoder().withoutPadding()
private val decoder = Base64.getUrlDecoder()
