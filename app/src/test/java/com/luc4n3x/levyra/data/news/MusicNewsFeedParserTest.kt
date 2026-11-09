package com.luc4n3x.levyra.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicNewsFeedParserTest {
    @Test
    fun readsMediaImageAndHttpsLink() {
        val xml = """<rss xmlns:media="http://search.yahoo.com/mrss/"><channel><item>
            <title>Album &amp; tour</title><link>https://example.com/a</link>
            <pubDate>Wed, 04 Nov 2026 10:00:00 GMT</pubDate>
            <media:content url="https://img.example.com/a.jpg" medium="image"/></item></channel></rss>"""
        val story = MusicNewsFeedParser.parse(xml, source = "Example").single()
        assertEquals("Album & tour", story.title)
        assertEquals("https://example.com/a", story.url)
        assertEquals("https://img.example.com/a.jpg", story.imageUrl)
        assertEquals("Example", story.source)
        assertEquals(1793786400000L, story.publishedAtMs)
    }

    @Test
    fun skipsVideoMediaContentAndUsesThumbnail() {
        val xml = """<rss xmlns:media="http://search.yahoo.com/mrss/"><channel><item>
            <title>Video</title><link>https://example.com/v</link>
            <media:content url="https://www.youtube.com/embed/abc" duration="187"/>
            <media:thumbnail url="https://img.example.com/v.jpg"/></item></channel></rss>"""
        assertEquals("https://img.example.com/v.jpg", MusicNewsFeedParser.parse(xml, "Example").single().imageUrl)
    }

    @Test
    fun readsImageEnclosure() {
        val xml = """<rss><channel><item><title>Enclosure</title><link>https://example.com/e</link>
            <enclosure url="https://img.example.com/e.jpg" length="10" type="image/jpeg"/>
            <enclosure url="https://example.com/e.mp3" type="audio/mpeg"/></item></channel></rss>"""
        assertEquals("https://img.example.com/e.jpg", MusicNewsFeedParser.parse(xml, "Example").single().imageUrl)
    }

    @Test
    fun ignoresNonImageEnclosure() {
        val xml = """<rss><channel><item><title>Audio</title><link>https://example.com/p</link>
            <enclosure url="https://example.com/p.mp3" type="audio/mpeg"/></item></channel></rss>"""
        assertEquals("", MusicNewsFeedParser.parse(xml, "Example").single().imageUrl)
    }

    @Test
    fun readsFirstInlineImageFromEscapedDescription() {
        val xml = """<rss><channel><item><title>Inline</title><link>https://example.com/i</link>
            <description>&lt;p&gt;&lt;img width="50" src="https://img.example.com/i.jpg?a=1&amp;amp;b=2" /&gt;Text&lt;/p&gt;</description>
            </item></channel></rss>"""
        assertEquals("https://img.example.com/i.jpg?a=1&b=2", MusicNewsFeedParser.parse(xml, "Example").single().imageUrl)
    }

    @Test
    fun readsInlineImageFromContentEncodedCdata() {
        val xml = """<rss xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><item>
            <title>Cdata</title><link>https://example.com/c</link><description>No picture</description>
            <content:encoded><![CDATA[<figure><img class="x" src='https://img.example.com/c.png'></figure>]]></content:encoded>
            </item></channel></rss>"""
        assertEquals("https://img.example.com/c.png", MusicNewsFeedParser.parse(xml, "Example").single().imageUrl)
    }

    @Test
    fun dropsInsecureLinksAndBlanksInsecureImages() {
        val xml = """<rss xmlns:media="http://search.yahoo.com/mrss/"><channel>
            <item><title>Plain link</title><link>http://example.com/a</link></item>
            <item><title>Plain image</title><link>https://example.com/b</link>
            <media:thumbnail url="http://img.example.com/b.jpg"/></item>
            <item><title>Script link</title><link>javascript:alert(1)</link></item>
            </channel></rss>"""
        val story = MusicNewsFeedParser.parse(xml, "Example").single()
        assertEquals("https://example.com/b", story.url)
        assertEquals("", story.imageUrl)
    }

    @Test
    fun rejectsDoctypeWithoutExpandingEntities() {
        val xml = """<?xml version="1.0"?><!DOCTYPE rss [<!ENTITY boom "expanded">]>
            <rss><channel><item><title>&boom;</title><link>https://example.com/x</link></item></channel></rss>"""
        assertTrue(MusicNewsFeedParser.parse(xml, "Example").isEmpty())
    }

    @Test
    fun rejectsExternalEntityDeclarations() {
        val xml = """<?xml version="1.0"?><!DOCTYPE rss [<!ENTITY file SYSTEM "file:///etc/passwd">]>
            <rss><channel><item><title>&file;</title><link>https://example.com/x</link></item></channel></rss>"""
        assertTrue(MusicNewsFeedParser.parse(xml, "Example").isEmpty())
    }

    @Test
    fun malformedXmlReturnsEmpty() {
        assertTrue(MusicNewsFeedParser.parse("<rss><channel><item>", "Example").isEmpty())
        assertTrue(MusicNewsFeedParser.parse("not xml", "Example").isEmpty())
    }

    @Test
    fun readsAtomEntry() {
        val xml = """<feed xmlns="http://www.w3.org/2005/Atom"><title>Feed</title>
            <entry><title type="html">Atom &lt;b&gt;story&lt;/b&gt;</title>
            <link rel="self" href="https://example.com/self"/>
            <link rel="alternate" href="https://example.com/atom"/>
            <updated>2026-11-04T10:00:00+01:00</updated>
            <summary type="html">&lt;img src="https://img.example.com/atom.webp"&gt;</summary>
            </entry></feed>"""
        val story = MusicNewsFeedParser.parse(xml, "Example").single()
        assertEquals("Atom story", story.title)
        assertEquals("https://example.com/atom", story.url)
        assertEquals("https://img.example.com/atom.webp", story.imageUrl)
        assertEquals(1793786400000L - 3_600_000L, story.publishedAtMs)
    }

    @Test
    fun decodesDoubleEncodedEntitiesInTitle() {
        val xml = """<rss><channel><item><title>Rock &amp;amp; roll &amp;#8217;s &amp;quot;best&amp;quot; &amp;#39;26</title>
            <link>https://example.com/t</link></item></channel></rss>"""
        assertEquals("Rock & roll ’s \"best\" '26", MusicNewsFeedParser.parse(xml, "Example").single().title)
    }

    @Test
    fun parsesDatesAndFallsBackToZero() {
        assertEquals(1793786400000L, MusicNewsFeedParser.parseDate("Wed, 04 Nov 2026 10:00:00 +0000"))
        assertEquals(1793786400000L - 3L * 3_600_000L, MusicNewsFeedParser.parseDate("Wed, 04 Nov 2026 10:00:00 +0300"))
        assertEquals(1793786400000L, MusicNewsFeedParser.parseDate("2026-11-04T10:00:00Z"))
        assertEquals(0L, MusicNewsFeedParser.parseDate("yesterday"))
        val xml = """<rss><channel><item><title>Undated</title><link>https://example.com/u</link>
            <pubDate>sometime</pubDate></item></channel></rss>"""
        assertEquals(0L, MusicNewsFeedParser.parse(xml, "Example").single().publishedAtMs)
    }

    @Test
    fun capsAtThirtyStories() {
        val items = (1..45).joinToString("") { "<item><title>Story $it</title><link>https://example.com/$it</link></item>" }
        assertEquals(30, MusicNewsFeedParser.parse("<rss><channel>$items</channel></rss>", "Example").size)
    }

    @Test
    fun codecRoundTripsAndRevalidatesHttps() {
        val page = MusicNewsPage(
            stories = listOf(
                MusicNewsStory("https://example.com/a", "A", "Example", "https://img.example.com/a.jpg", 5L),
                MusicNewsStory("http://example.com/b", "B", "Example", "", 4L),
                MusicNewsStory("https://example.com/c", "C", "Example", "http://img.example.com/c.jpg", 3L)
            ),
            fetchedAtMs = 99L
        )
        val decoded = MusicNewsCodec.decode(MusicNewsCodec.encode(page))!!
        assertEquals(99L, decoded.fetchedAtMs)
        assertEquals(listOf("https://example.com/a", "https://example.com/c"), decoded.stories.map { it.url })
        assertEquals(page.stories[0], decoded.stories[0])
        assertEquals("", decoded.stories[1].imageUrl)
        assertNull(MusicNewsCodec.decode("{broken"))
    }
}
