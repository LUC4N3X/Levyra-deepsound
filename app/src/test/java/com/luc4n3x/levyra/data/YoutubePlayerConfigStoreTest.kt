package com.luc4n3x.levyra.data

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory

class YoutubePlayerConfigStoreTest {
    private val cacheDir: File = createTempDirectory("levyra-player-config").toFile()
    private val remoteFile = File(cacheDir, "player_configs_remote.json")
    private val metadataFile = File(cacheDir, "player_configs_meta.json")
    private val provisionalFile = File(cacheDir, "player_configs_provisional.json")
    private val provisionalMetadataFile = File(cacheDir, "player_configs_provisional_meta.json")
    private val requests = mutableListOf<Request>()
    private var now = 10_000_000L

    private val upstream = YoutubePlayerConfigSource("zemer-upstream", "https://upstream.test/player_configs.json")
    private val mirror = YoutubePlayerConfigSource("levyra-verified-mirror", "https://mirror.test/player_configs.json")

    private val bundledTable = table(entry(BUNDLED_HASH, 20000))
    private val remoteTable = table(entry(BUNDLED_HASH, 20001), entry(REMOTE_HASH, 20002))
    private val newerRemoteTable = table(entry(NEWER_HASH, 20003))

    @After
    fun cleanUp() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun validUpstreamIsPublishedAndPersistedAsLastKnownGood() = runBlocking {
        val store = store(respond(upstream to ok(remoteTable, etag = "\"v1\"")))
        val initialEpoch = store.epoch

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals(20001, store.configFor(BUNDLED_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertNotNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
        assertEquals(initialEpoch + 1, store.epoch)
        assertEquals(remoteTable, remoteFile.readText())
        val metadata = JSONObject(metadataFile.readText())
        assertEquals("\"v1\"", metadata.getString("etag"))
        assertEquals(upstream.id, metadata.getString("sourceId"))
        assertEquals(now, metadata.getLong("checkedAtMs"))
    }

    @Test
    fun upstreamOfflineKeepsBundledPlaybackAvailable() = runBlocking {
        val store = store(offline())

        assertFalse(store.refresh(force = true, reason = "test"))
        assertEquals(YoutubeStreamRefreshResult.NETWORK_FAILURE, store.refreshAfterStreamRejection())

        assertEquals(20000, store.configFor(BUNDLED_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
        assertFalse(remoteFile.exists())
    }

    @Test
    fun upstreamOfflineKeepsLastKnownGood() = runBlocking {
        seedLastKnownGood()
        val store = store(offline())

        assertFalse(store.refresh(force = true, reason = "test"))

        assertNotNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
        assertEquals(remoteTable, remoteFile.readText())
    }

    @Test
    fun unsuccessfulHttpDoesNotReplaceLastKnownGood() {
        assertRejectedResponseKeepsLastKnownGood(ok(newerRemoteTable).copyWithCode(503))
        assertRejectedResponseKeepsLastKnownGood(ok(newerRemoteTable).copyWithCode(404))
    }

    @Test
    fun corruptJsonDoesNotReplaceLastKnownGood() {
        assertRejectedResponseKeepsLastKnownGood(ok("{\"schemaVersion\":1,\"players\":{\"$NEWER_HASH\":"))
        assertRejectedResponseKeepsLastKnownGood(ok("<html>rate limited by upstream proxy</html>"))
    }

    @Test
    fun unsupportedSchemaDoesNotReplaceLastKnownGood() {
        assertRejectedResponseKeepsLastKnownGood(ok(table(entry(NEWER_HASH, 20003), schema = "2")))
        assertRejectedResponseKeepsLastKnownGood(ok(table(entry(NEWER_HASH, 20003), schema = "\"1\"")))
        assertRejectedResponseKeepsLastKnownGood(ok("{\"schemaVersion\":1,\"entries\":{\"$NEWER_HASH\":{}}}"))
    }

    @Test
    fun invalidHashesAndAliasesDoNotReplaceLastKnownGood() {
        assertRejectedResponseKeepsLastKnownGood(
            ok(table(entry(NEWER_HASH, 20003), entry("abcdef01", 20004, aliases = "[\"$NEWER_HASH\"]")))
        )
        assertRejectedResponseKeepsLastKnownGood(
            ok(table(entry(NEWER_HASH, 20003, aliases = "[\"$NEWER_HASH\"]")))
        )
        assertRejectedResponseKeepsLastKnownGood(
            ok(table(entry("NOT-A-HASH", 20003), entry("abcdef01", 20004, aliases = "[\"zz\"]")))
        )
    }

    @Test
    fun invalidRemoteConfigNeverChangesEpochOrPublishedTable() = runBlocking {
        seedLastKnownGood()
        val store = store(respond(upstream to ok("{\"schemaVersion\":3,\"players\":{}}")))
        store.configFor(REMOTE_HASH, refreshUnknown = false)
        val epoch = store.epoch

        assertFalse(store.refresh(force = true, reason = "test"))
        assertEquals(YoutubeStreamRefreshResult.UNCHANGED, store.refreshAfterStreamRejection())

        assertEquals(epoch, store.epoch)
        assertEquals(20002, store.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
    }

    @Test
    fun lastKnownGoodIsRestoredAfterRestartWithoutNetwork() = runBlocking {
        seedLastKnownGood()

        val restarted = store(offline())

        assertEquals(20001, restarted.configFor(BUNDLED_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertEquals(20002, restarted.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun tamperedLastKnownGoodFallsBackToBundledConfig() = runBlocking {
        seedLastKnownGood()
        remoteFile.writeText(newerRemoteTable)

        val restarted = store(offline())

        assertEquals(20000, restarted.configFor(BUNDLED_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertNull(restarted.configFor(REMOTE_HASH, refreshUnknown = false))
        assertNull(restarted.configFor(NEWER_HASH, refreshUnknown = false))
        assertFalse(remoteFile.exists())
    }

    @Test
    fun bundledConfigIsFinalFallbackWhenNoRemoteSourceIsUsable() = runBlocking {
        val store = store(
            respond(upstream to ok("not json at all, only noise"), mirror to ok(newerRemoteTable).copyWithCode(500)),
            sources = listOf(upstream, mirror)
        )

        assertFalse(store.refresh(force = true, reason = "test"))

        assertEquals(20000, store.configFor(BUNDLED_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertNull(store.configFor(NEWER_HASH, refreshUnknown = false))
        assertFalse(remoteFile.exists())
    }

    @Test
    fun freshCacheSkipsNetworkUntilTtlExpires() = runBlocking {
        seedLastKnownGood()
        val store = store(respond(upstream to ok(newerRemoteTable)))

        now += 60L * 60L * 1000L
        assertFalse(store.refresh(force = false, reason = "prewarm"))
        assertTrue(requests.isEmpty())

        now += 6L * 60L * 60L * 1000L
        assertTrue(store.refresh(force = false, reason = "prewarm"))
        assertEquals(1, requests.size)
        assertNotNull(store.configFor(NEWER_HASH, refreshUnknown = false))
    }

    @Test
    fun futureCacheTimestampDoesNotSuppressRefresh() = runBlocking {
        seedLastKnownGood()
        val metadata = JSONObject(metadataFile.readText()).put("checkedAtMs", now + 24L * 60L * 60L * 1000L)
        metadataFile.writeText(metadata.toString())
        val store = store(respond(upstream to ok(newerRemoteTable)))

        assertTrue(store.refresh(force = false, reason = "prewarm"))

        assertEquals(1, requests.size)
        assertEquals(now, JSONObject(metadataFile.readText()).getLong("checkedAtMs"))
    }

    @Test
    fun conditionalRequestKeepsLastKnownGoodOnNotModified() = runBlocking {
        seedLastKnownGood(etag = "\"v1\"")
        now += 7L * 60L * 60L * 1000L
        val store = store(respond(upstream to notModified()))

        assertFalse(store.refresh(force = false, reason = "prewarm"))

        assertEquals("\"v1\"", requests.single().header("If-None-Match"))
        assertNotNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
        assertEquals(now, JSONObject(metadataFile.readText()).getLong("checkedAtMs"))
        assertEquals("\"v1\"", JSONObject(metadataFile.readText()).getString("etag"))
    }

    @Test
    fun newBodyWithoutEtagClearsPreviousEtag() = runBlocking {
        seedLastKnownGood(etag = "\"v1\"")
        val store = store(respond(upstream to ok(newerRemoteTable)))

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals(newerRemoteTable, remoteFile.readText())
        assertNotNull(store.configFor(NEWER_HASH, refreshUnknown = false))
        val metadata = JSONObject(metadataFile.readText())
        assertEquals("", metadata.getString("etag"))
        assertEquals(upstream.id, metadata.getString("sourceId"))

        assertFalse(store.refresh(force = true, reason = "test"))

        assertEquals(2, requests.size)
        assertNull(requests[1].header("If-None-Match"))
    }

    @Test
    fun missingLastKnownGoodNeverSendsStaleEtag() = runBlocking {
        seedLastKnownGood(etag = "\"v1\"")
        remoteFile.delete()
        val store = store(respond(upstream to ok(newerRemoteTable, etag = "\"v2\"")))

        assertTrue(store.refresh(force = true, reason = "test"))

        assertNull(requests.single().header("If-None-Match"))
        assertNotNull(store.configFor(NEWER_HASH, refreshUnknown = false))
    }

    @Test
    fun unsolicitedNotModifiedIsRejected() = runBlocking {
        val store = store(respond(upstream to notModified()))

        assertFalse(store.refresh(force = true, reason = "test"))

        assertEquals(YoutubeStreamRefreshResult.UNCHANGED, store.refreshAfterStreamRejection())
        assertNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
    }

    @Test
    fun legacyVerifiedMetadataWithoutSourceKeepsMirrorEtag() = runBlocking {
        seedLastKnownGood(etag = "\"legacy\"", source = mirror)
        val metadata = JSONObject(metadataFile.readText())
        metadata.remove("sourceId")
        metadataFile.writeText(metadata.toString())
        val store = store(respond(mirror to notModified()), sources = listOf(mirror))

        assertFalse(store.refresh(force = true, reason = "test"))

        assertEquals("\"legacy\"", requests.single().header("If-None-Match"))
        assertNotNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
    }

    @Test
    fun unknownPlayerTriggersValidatedRefresh() = runBlocking {
        val store = store(respond(upstream to ok(remoteTable)))

        assertEquals(20002, store.configFor(REMOTE_HASH, refreshUnknown = true)?.signatureTimestamp)
        assertEquals(1, requests.size)
    }

    @Test
    fun mirrorIsUsedOnlyAfterUpstreamFailureAndPassesSameValidation() = runBlocking {
        val store = store(
            respond(mirror to ok(remoteTable, etag = "\"m1\"")),
            sources = listOf(upstream, mirror)
        )

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals(listOf(upstream.url, mirror.url), requests.map { it.url.toString() })
        assertNotNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
        val metadata = JSONObject(metadataFile.readText())
        assertEquals(mirror.id, metadata.getString("sourceId"))
        assertEquals("\"m1\"", metadata.getString("etag"))
    }

    @Test
    fun mirrorIsNotContactedWhenUpstreamSucceeds() = runBlocking {
        val store = store(
            respond(upstream to ok(remoteTable), mirror to ok(newerRemoteTable)),
            sources = listOf(upstream, mirror)
        )

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals(listOf(upstream.url), requests.map { it.url.toString() })
        assertNull(store.configFor(NEWER_HASH, refreshUnknown = false))
    }

    @Test
    fun invalidMirrorDoesNotReplaceLastKnownGood() = runBlocking {
        seedLastKnownGood()
        val store = store(
            respond(mirror to ok(table(entry(NEWER_HASH, 20003), schema = "9"))),
            sources = listOf(upstream, mirror)
        )

        assertFalse(store.refresh(force = true, reason = "test"))

        assertNotNull(store.configFor(REMOTE_HASH, refreshUnknown = false))
        assertNull(store.configFor(NEWER_HASH, refreshUnknown = false))
        assertEquals(remoteTable, remoteFile.readText())
    }

    @Test
    fun etagFromOneSourceIsNeverSentToAnother() = runBlocking {
        seedLastKnownGood(etag = "\"upstream\"")
        val store = store(
            respond(mirror to ok(newerRemoteTable)),
            sources = listOf(upstream, mirror)
        )

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals("\"upstream\"", requests[0].header("If-None-Match"))
        assertNull(requests[1].header("If-None-Match"))
    }

    @Test
    fun mirrorEtagIsSentOnlyToMirrorAndNotModifiedKeepsLastKnownGood() = runBlocking {
        val seeding = store(respond(mirror to ok(remoteTable, etag = "\"m1\"")), sources = listOf(upstream, mirror))
        assertTrue(seeding.refresh(force = true, reason = "seed"))
        requests.clear()
        now += 60_000L
        val store = store(respond(mirror to notModified()), sources = listOf(upstream, mirror))
        store.configFor(REMOTE_HASH, refreshUnknown = false)
        val epoch = store.epoch

        assertFalse(store.refresh(force = true, reason = "test"))

        assertEquals(listOf(upstream.url, mirror.url), requests.map { it.url.toString() })
        assertNull(requests[0].header("If-None-Match"))
        assertEquals("\"m1\"", requests[1].header("If-None-Match"))
        assertEquals(epoch, store.epoch)
        assertEquals(20002, store.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertEquals(remoteTable, remoteFile.readText())
        val metadata = JSONObject(metadataFile.readText())
        assertEquals(mirror.id, metadata.getString("sourceId"))
        assertEquals("\"m1\"", metadata.getString("etag"))
        assertEquals(now, metadata.getLong("checkedAtMs"))
    }

    @Test
    fun activeSourcesPreferVerifiedMirrorThenProvisionalRawUpstreams() {
        assertEquals(
            listOf(
                YoutubePlayerConfigSources.LEVYRA_VERIFIED_MIRROR,
                YoutubePlayerConfigSources.ZEMER_UPSTREAM,
                YoutubePlayerConfigSources.FARADAY_UPSTREAM
            ),
            YoutubePlayerConfigSources.active
        )
        assertEquals(
            "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/main/app/src/main/assets/player_configs.json",
            YoutubePlayerConfigSources.LEVYRA_VERIFIED_MIRROR.url
        )
        assertEquals(YoutubePlayerConfigTrust.VERIFIED, YoutubePlayerConfigSources.LEVYRA_VERIFIED_MIRROR.trust)
        assertEquals(
            "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json",
            YoutubePlayerConfigSources.ZEMER_UPSTREAM.url
        )
        assertEquals(YoutubePlayerConfigTrust.PROVISIONAL, YoutubePlayerConfigSources.ZEMER_UPSTREAM.trust)
        assertEquals(
            "https://raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player_configs.json",
            YoutubePlayerConfigSources.FARADAY_UPSTREAM.url
        )
        assertEquals(YoutubePlayerConfigTrust.PROVISIONAL, YoutubePlayerConfigSources.FARADAY_UPSTREAM.trust)
    }

    @Test
    fun defaultSourcesPreferVerifiedMirrorWithoutContactingRawUpstreams() = runBlocking {
        val mirror = YoutubePlayerConfigSources.LEVYRA_VERIFIED_MIRROR
        val zemer = YoutubePlayerConfigSources.ZEMER_UPSTREAM
        val store = store(
            respond(mirror to ok(remoteTable, etag = "\"m1\""), zemer to ok(newerRemoteTable)),
            sources = YoutubePlayerConfigSources.active
        )

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals(listOf(mirror.url), requests.map { it.url.toString() })
        assertNull(store.configFor(NEWER_HASH, refreshUnknown = false))
        val metadata = JSONObject(metadataFile.readText())
        assertEquals(mirror.id, metadata.getString("sourceId"))
        assertEquals("\"m1\"", metadata.getString("etag"))
    }

    @Test
    fun defaultSourcesUseProvisionalRawUpstreamWhenVerifiedMirrorFails() = runBlocking {
        val mirror = YoutubePlayerConfigSources.LEVYRA_VERIFIED_MIRROR
        val zemer = YoutubePlayerConfigSources.ZEMER_UPSTREAM
        val store = store(
            respond(mirror to ok(remoteTable).copyWithCode(503), zemer to ok(remoteTable, etag = "\"z1\"")),
            sources = YoutubePlayerConfigSources.active
        )

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals(listOf(mirror.url, zemer.url), requests.map { it.url.toString() })
        assertEquals(20002, store.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertFalse(metadataFile.exists())
        assertEquals(remoteTable, provisionalFile.readText())
        assertEquals(zemer.id, JSONObject(provisionalMetadataFile.readText()).getString("sourceId"))
    }

    @Test
    fun provisionalRawSourceDoesNotWriteVerifiedLastKnownGood() = runBlocking {
        val raw = YoutubePlayerConfigSource(
            "zemer-upstream",
            "https://raw.test/player_configs.json",
            YoutubePlayerConfigTrust.PROVISIONAL
        )
        val store = store(respond(raw to ok(remoteTable)), sources = listOf(raw))

        assertTrue(store.refresh(force = true, reason = "test"))

        assertEquals(20002, store.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertFalse(remoteFile.exists())
        assertFalse(metadataFile.exists())
        assertEquals(remoteTable, provisionalFile.readText())
    }

    @Test
    fun provisionalConfigNeverOverridesVerifiedLastKnownGood() = runBlocking {
        seedLastKnownGood()
        val raw = YoutubePlayerConfigSource(
            "zemer-upstream",
            "https://raw.test/player_configs.json",
            YoutubePlayerConfigTrust.PROVISIONAL
        )
        val store = store(respond(raw to ok(table(entry(REMOTE_HASH, 99999)))), sources = listOf(raw))
        store.configFor(REMOTE_HASH, refreshUnknown = false)

        assertFalse(store.refresh(force = true, reason = "test"))

        assertEquals(20002, store.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertEquals(remoteTable, remoteFile.readText())
        assertEquals(table(entry(REMOTE_HASH, 99999)), provisionalFile.readText())
    }

    @Test
    fun verifiedRefreshStillWorksAfterProvisionalFallback() = runBlocking {
        val raw = YoutubePlayerConfigSource(
            "zemer-upstream",
            "https://raw.test/player_configs.json",
            YoutubePlayerConfigTrust.PROVISIONAL
        )
        val first = store(respond(raw to ok(remoteTable)), sources = listOf(raw))
        assertTrue(first.refresh(force = true, reason = "test"))
        assertEquals(remoteTable, provisionalFile.readText())

        requests.clear()
        val mirror = YoutubePlayerConfigSource(
            "levyra-verified-mirror",
            "https://mirror.test/player_configs.json",
            YoutubePlayerConfigTrust.VERIFIED
        )
        val restarted = store(respond(mirror to ok(newerRemoteTable)), sources = listOf(mirror))

        assertTrue(restarted.refresh(force = true, reason = "test"))

        assertNotNull(restarted.configFor(NEWER_HASH, refreshUnknown = false))
        assertNotNull(restarted.configFor(REMOTE_HASH, refreshUnknown = false))
        assertEquals(newerRemoteTable, remoteFile.readText())
    }

    @Test(expected = IllegalArgumentException::class)
    fun configSourcesMustUseHttps() {
        YoutubePlayerConfigSource("insecure", "http://mirror.test/player_configs.json")
    }

    private fun assertRejectedResponseKeepsLastKnownGood(response: FakeResponse) = runBlocking {
        cacheDir.deleteRecursively()
        requests.clear()
        seedLastKnownGood()
        val metadataBefore = metadataFile.readText()
        val store = store(respond(upstream to response))

        assertFalse(store.refresh(force = true, reason = "test"))

        assertEquals(1, requests.size)
        assertEquals(20002, store.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
        assertNull(store.configFor(NEWER_HASH, refreshUnknown = false))
        assertEquals(remoteTable, remoteFile.readText())
        assertEquals(metadataBefore, metadataFile.readText())

        val restarted = store(offline())
        assertEquals(20002, restarted.configFor(REMOTE_HASH, refreshUnknown = false)?.signatureTimestamp)
    }

    private fun seedLastKnownGood(
        etag: String = "",
        source: YoutubePlayerConfigSource = upstream
    ) = runBlocking {
        val seeding = store(respond(source to ok(remoteTable, etag = etag)), sources = listOf(source))
        assertTrue(seeding.refresh(force = true, reason = "seed"))
        requests.clear()
    }

    private fun store(
        handler: (Request) -> Response,
        sources: List<YoutubePlayerConfigSource> = listOf(upstream)
    ): YoutubePlayerConfigStore {
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                synchronized(requests) { requests += request }
                handler(request)
            })
            .build()
        return YoutubePlayerConfigStore(
            httpClient = client,
            cacheDir = cacheDir,
            bundledConfigText = { bundledTable },
            sources = sources,
            clock = { now }
        )
    }

    private fun offline(): (Request) -> Response = { throw IOException("offline") }

    private fun respond(vararg routes: Pair<YoutubePlayerConfigSource, FakeResponse>): (Request) -> Response {
        val byUrl = routes.associate { (source, response) -> source.url to response }
        return { request ->
            val response = byUrl[request.url.toString()] ?: throw IOException("unreachable ${request.url}")
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(response.code)
                .message("test")
                .apply { if (response.etag.isNotEmpty()) header("ETag", response.etag) }
                .body(response.body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    private data class FakeResponse(val code: Int, val body: String, val etag: String) {
        fun copyWithCode(code: Int) = copy(code = code)
    }

    private fun ok(body: String, etag: String = "") = FakeResponse(200, body, etag)

    private fun notModified() = FakeResponse(304, "", "")

    private fun table(vararg entries: String, schema: String = "1"): String =
        "{\"schemaVersion\":$schema,\"players\":{${entries.joinToString(",")}}}"

    private fun entry(hash: String, sts: Int, aliases: String = "[]"): String =
        "\"$hash\":{\"sig\":\"Ab(1,2,INPUT)\",\"nClass\":\"Yx\",\"sts\":$sts,\"aliases\":$aliases}"

    private companion object {
        const val BUNDLED_HASH = "11111111"
        const val REMOTE_HASH = "22222222"
        const val NEWER_HASH = "33333333"
    }
}
