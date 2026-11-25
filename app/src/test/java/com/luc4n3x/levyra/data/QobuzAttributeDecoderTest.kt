package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class QobuzAttributeDecoderTest {
    @Test
    fun decodesEveryQobuzEntityInOnePass() {
        assertEquals(
            """{"albums":{"title":"Rock & Roll <Live> it's 'x'"}}""",
            decodeQobuzAttribute("{&quot;albums&quot;:{&quot;title&quot;:&quot;Rock &amp; Roll &lt;Live&gt; it&#39;s &apos;x&apos;&quot;}}")
        )
    }

    @Test
    fun encodedAmpersandIsNotDecodedTwice() {
        assertEquals("&quot;", decodeQobuzAttribute("&amp;quot;"))
    }

    @Test
    fun unknownEntitiesAndBareAmpersandsStayUntouched() {
        assertEquals("AT&T &nbsp; & &unknown;", decodeQobuzAttribute("AT&T &nbsp; & &unknown;"))
    }

    @Test
    fun valueWithoutEntitiesIsReturnedAsIs() {
        val value = "plain value"
        assertSame(value, decodeQobuzAttribute(value))
    }

    @Test
    fun decodedLivePropsParseAsJson() {
        val decoded = decodeQobuzAttribute("{&quot;albums&quot;:{&quot;1&quot;:{&quot;upc&quot;:&quot;0123456789012&quot;}}}")
        assertEquals("0123456789012", JSONObject(decoded).getJSONObject("albums").getJSONObject("1").getString("upc"))
    }
}
