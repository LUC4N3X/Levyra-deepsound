package com.luc4n3x.levyra.data

import java.util.Base64
import java.util.zip.Deflater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouLyricsTest {
    private val key = byteArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, -50, -46, 110, 105)

    private fun encrypt(text: String): String {
        val deflater = Deflater()
        deflater.setInput(text.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val buffer = ByteArray(64 * 1024)
        val size = deflater.deflate(buffer)
        deflater.end()
        val xored = ByteArray(size) { (buffer[it].toInt() xor key[it % key.size].toInt()).toByte() }
        return Base64.getEncoder().encodeToString("krc1".toByteArray(Charsets.US_ASCII) + xored)
    }

    @Test
    fun `parses candidates and keeps plausible durations`() {
        val body = """{"candidates":[
            {"id":"1","accesskey":"A","song":"光年之外","singer":"邓紫棋","duration":235000},
            {"id":"2","accesskey":"B","song":"光年之外","singer":"Cover","duration":180000},
            {"id":"","accesskey":"C","song":"broken","singer":"x","duration":1}
        ]}"""
        val candidates = parseKugouCandidates(body)
        assertEquals(listOf("1", "2"), candidates.map { it.id })
        assertEquals(listOf("1"), kugouShortlist(candidates, 236L).map { it.id })
    }

    @Test
    fun `decrypts krc into word timed lyrics`() {
        val krc = "[ar:邓紫棋]\r\n[ti:光年之外]\r\n[language:eyJjb250ZW50IjpbXSwidmVyc2lvbiI6MX0=]\r\n" +
            "[12000,2000]<0,500,0>感<500,500,0>受<1000,500,0>停<1500,500,0>在\r\n" +
            "[14500,1500]<0,750,0>我<750,750,0>们\r\n"
        val result = parseKugouDownload("""{"content":"${encrypt(krc)}"}""")!!
        assertTrue(result.synced)
        assertEquals("Kugou", result.provider)
        val line = result.lines.first { it.text.isNotBlank() }
        assertEquals("感受停在", line.text)
        assertEquals(12_000L, line.startMs)
        assertEquals(4, line.words.size)
    }

    @Test
    fun `rejects payloads that are not krc`() {
        assertNull(decryptKugouKrc("nope".toByteArray()))
        assertNull(parseKugouDownload("""{"content":""}"""))
        assertNull(decryptKugouKrc("krc1".toByteArray() + ByteArray(32) { 7 }))
    }
}
