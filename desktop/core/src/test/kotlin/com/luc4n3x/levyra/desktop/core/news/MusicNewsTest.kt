package com.luc4n3x.levyra.desktop.core.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicNewsTest {

    @Test
    fun parsesRssItemsWithImages() {
        val xml = """<?xml version="1.0"?><rss xmlns:media="http://search.yahoo.com/mrss/"><channel>
            <item><title>Nuovo album &amp; tour</title><link>https://site.it/a</link><pubDate>Wed, 26 Nov 2026 10:00:00 +0000</pubDate>
            <media:content url="https://img.site.it/a.jpg"/></item>
            <item><title>No https</title><link>http://site.it/b</link></item>
            <item><title>Inline</title><link>https://site.it/c</link><description><![CDATA[<p><img src="https://img.site.it/c.jpg"></p>]]></description></item>
        </channel></rss>"""
        val stories = MusicNews.parse(xml, "Rockol")
        assertEquals(listOf("https://site.it/a", "https://site.it/c"), stories.map { it.url })
        assertEquals("Nuovo album & tour", stories.first().title)
        assertEquals("https://img.site.it/a.jpg", stories.first().imageUrl)
        assertEquals("https://img.site.it/c.jpg", stories[1].imageUrl)
        assertTrue(stories.first().publishedAtMs > 0L)
    }

    @Test
    fun rejectsDoctypeEntities() {
        val xml = """<?xml version="1.0"?><!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]><rss><channel><item><title>&x;</title><link>https://a/b</link></item></channel></rss>"""
        assertTrue(MusicNews.parse(xml, "X").isEmpty())
    }

    @Test
    fun picksLanguageSources() {
        assertEquals("Rockol", MusicNews.sources("it").first().name)
        assertEquals("Pitchfork", MusicNews.sources("ja").first().name)
    }
}
