package com.luc4n3x.levyra.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicNewsArticleParserTest {
    private val story = MusicNewsStory(
        url = "https://www.rollingstone.it/musica/news-musica/slug/1045820/",
        title = "Feed title",
        source = "Rolling Stone Italia",
        imageUrl = "https://img.example.com/feed.jpg",
        publishedAtMs = 1L,
        articleId = "1045820"
    )

    @Test
    fun wordpressPostBecomesOrderedBlocks() {
        val json = """
            {"title":{"rendered":"Titolo &#8220;vero&#8221;"},
             "content":{"rendered":"<figure><img src=\"https://img.example.com/hero-1024x683.jpg\"></figure><p>Primo <a href=\"https://x.example/a\">link</a> &amp; testo.</p><h2>Sezione</h2><blockquote><p>Una citazione</p></blockquote><figure class=\"wp-block-image\"><img data-src=\"https://img.example.com/body.jpg\" src=\"data:image/gif;base64,AA\"><figcaption>Didascalia</figcaption></figure><ul><li>Uno</li><li>Due</li></ul><figure class=\"wp-block-embed\"><iframe src=\"https://www.youtube.com/embed/x\"></iframe></figure><p> </p>"},
             "_embedded":{"author":[{"name":"Claudio Todesco"}],"wp:featuredmedia":[{"source_url":"https://img.example.com/hero.jpg"}]}}
        """.trimIndent()

        val article = MusicNewsArticleParser.wordpress(json, story)!!

        assertEquals("Titolo “vero”", article.title)
        assertEquals("Claudio Todesco", article.author)
        assertEquals("https://img.example.com/hero.jpg", article.heroImageUrl)
        assertEquals(
            listOf(
                MusicNewsBlock.Paragraph("Primo <a href=\"https://x.example/a\">link</a> &amp; testo."),
                MusicNewsBlock.Heading("Sezione"),
                MusicNewsBlock.Quote("Una citazione"),
                MusicNewsBlock.Image("https://img.example.com/body.jpg", "Didascalia"),
                MusicNewsBlock.Bullet("Uno"),
                MusicNewsBlock.Bullet("Due")
            ),
            article.blocks
        )
    }

    @Test
    fun relatedLinkListsAndInlineThumbnailsAreDropped() {
        val blocks = MusicNewsArticleParser.blocks(
            "<p>Testo <img src=\"https://img.example.com/t.jpg\">vero.</p>" +
                "<p>Il rapper<a href=\"https://x.example/r\"> Don Toliver </a>era in concerto.</p>" +
                "<ul><li>Data <a href=\"https://x.example/3\">biglietti</a></li></ul>" +
                "<h3>Le più lette</h3><ul><li><img src=\"https://img.example.com/a.jpg\"><a href=\"https://x.example/1\">Uno</a></li>" +
                "<li><a href=\"https://x.example/2\">Due</a></li></ul>"
        )

        assertEquals(
            listOf(
                MusicNewsBlock.Paragraph("Testo vero."),
                MusicNewsBlock.Paragraph("Il rapper <a href=\"https://x.example/r\">Don Toliver</a> era in concerto."),
                MusicNewsBlock.Bullet("Data <a href=\"https://x.example/3\">biglietti</a>")
            ),
            blocks
        )
    }

    @Test
    fun linkedTrackListInsideTheArticleIsKept() {
        val blocks = MusicNewsArticleParser.blocks(
            "<p>La tracklist:</p><ol><li><a href=\"https://x.example/1\">Intro</a></li><li><a href=\"https://x.example/2\">Outro</a></li></ol>" +
                "<p>Il disco esce venerdì.</p>"
        )

        assertEquals(
            listOf(
                MusicNewsBlock.Paragraph("La tracklist:"),
                MusicNewsBlock.Bullet("<a href=\"https://x.example/1\">Intro</a>"),
                MusicNewsBlock.Bullet("<a href=\"https://x.example/2\">Outro</a>"),
                MusicNewsBlock.Paragraph("Il disco esce venerdì.")
            ),
            blocks
        )
    }

    @Test
    fun onlyHttpsLinksSurviveInArticleText() {
        val blocks = MusicNewsArticleParser.blocks(
            "<p>A <a href=\"javascript:alert(1)\">uno</a>, <a href='intent://x#Intent;end'>due</a>, " +
                "<a href=javascript:x>tre</a> e <a class=\"x\" href=\"https://x.example/ok\" target=\"_blank\">quattro</a>.</p>"
        )

        assertEquals(
            listOf(MusicNewsBlock.Paragraph("A uno, due, tre</a> e <a href=\"https://x.example/ok\">quattro</a>.")),
            blocks
        )
    }

    @Test
    fun articleMadeOnlyOfLinkListsKeepsThem() {
        val blocks = MusicNewsArticleParser.blocks(
            "<h2>Da ascoltare</h2><ul><li><a href=\"https://x.example/1\">Uno</a></li><li><a href=\"https://x.example/2\">Due</a></li></ul>"
        )

        assertEquals(
            listOf(
                MusicNewsBlock.Heading("Da ascoltare"),
                MusicNewsBlock.Bullet("<a href=\"https://x.example/1\">Uno</a>"),
                MusicNewsBlock.Bullet("<a href=\"https://x.example/2\">Due</a>")
            ),
            blocks
        )
    }

    @Test
    fun bodyThatOnlyRepeatsTheHeroIsUnavailable() {
        val json = """
            {"content":{"rendered":"<figure><img src=\"https://img.example.com/2026/hero-800x600.jpg\"></figure>"},
             "_embedded":{"wp:featuredmedia":[{"source_url":"https://img.example.com/2026/hero.jpg"}]}}
        """.trimIndent()

        assertNull(MusicNewsArticleParser.wordpress(json, story))
    }

    @Test
    fun malformedGuidIdsAreIgnored() {
        val xml = """
            <rss version="2.0"><channel><item>
              <title>Story</title><link>https://consequence.net/2026/11/story/</link>
              <guid isPermaLink="false">https://consequence.net/?p=123-456</guid>
            </item></channel></rss>
        """.trimIndent()

        assertEquals("", MusicNewsFeedParser.parse(xml, "Consequence").single().articleId)
        assertEquals(
            "",
            MusicNewsCodec.decode("""{"fetchedAtMs":1,"stories":[{"url":"https://a.example/1","title":"T","articleId":"12a3"}]}""")!!
                .stories.single().articleId
        )
    }

    @Test
    fun nullJsonFieldsNeverBecomeLiteralText() {
        val article = MusicNewsArticleParser.wordpress(
            """{"title":{"rendered":null},"content":{"rendered":"<p>Testo</p>"},"_embedded":{"author":[{"name":null}]}}""",
            story
        )!!

        assertEquals(story.title, article.title)
        assertEquals("", article.author)
    }

    @Test
    fun wordpressPostWithoutBodyIsUnavailable() {
        assertNull(MusicNewsArticleParser.wordpress("""{"content":{"rendered":"<div></div>"}}""", story))
    }

    @Test
    fun preloadedStateTreeKeepsInlineFormattingAndImages() {
        val html = """
            <html><script>window.__PRELOADED_STATE__ = {"transformed":{"article":{
              "headerProps":{"dangerousHed":"Hed <em>Album</em>",
                "contributors":{"author":{"items":[{"name":"Hattie Lindert"},{"name":"Jazz Monroe"}]}},
                "lede":{"sources":{"xl":{"url":"https://media.example.com/lede.jpg"}}}},
              "body":["div",
                ["p",["a",{"href":"https://pitchfork.com/artists/x/"},"Artist"]," shares ",["em","Record"],"."],
                ["ad",{"position":"1"}],
                ["inline-embed",{"type":"image","props":{"dangerousCaption":"Photo <b>by</b> X","image":{"sources":{"xl":{"url":"https://media.example.com/body.jpg"}}}}}],
                ["inline-embed",{"type":"video","props":{"url":"https://www.youtube.com/watch?v=1"}}],
                ["h2","Tour dates"],
                ["p","Less < more & fun"]
              ]}}};</script></html>
        """.trimIndent()

        val article = MusicNewsArticleParser.preloadedState(html, story)!!

        assertEquals("Hed Album", article.title)
        assertEquals("Hattie Lindert, Jazz Monroe", article.author)
        assertEquals("https://media.example.com/lede.jpg", article.heroImageUrl)
        assertEquals(
            listOf(
                MusicNewsBlock.Paragraph("<a href=\"https://pitchfork.com/artists/x/\">Artist</a> shares <i>Record</i>."),
                MusicNewsBlock.Image("https://media.example.com/body.jpg", "Photo by X"),
                MusicNewsBlock.Heading("Tour dates"),
                MusicNewsBlock.Paragraph("Less &lt; more &amp; fun")
            ),
            article.blocks
        )
    }

    @Test
    fun jsonLdBodyReadsMarkedSectionAndMetadata() {
        val html = """
            <script type="application/ld+json">{"@context":"https://schema.org","@type":"NewsArticle","headline":"Rockol headline",
              "author":{"@type":"Person","name":"Redazione"},"image":["https://www.rockol.it/img/lead.jpg"]}</script>
            <p>Navigation teaser outside the body</p>
            <div class="body-text" data-rockol-news-detail-body>
              <p>Corpo dell'articolo.</p>
              <div class="rckl-promo"><script>ads()</script></div>
              <h3>Le date</h3>
              <p>Sabato 26 giugno.</p>
            </div></article><p>Related story</p>
        """.trimIndent()

        val article = MusicNewsArticleParser.jsonLdBody(html, story, "data-rockol-news-detail-body")!!

        assertEquals("Rockol headline", article.title)
        assertEquals("Redazione", article.author)
        assertEquals("https://www.rockol.it/img/lead.jpg", article.heroImageUrl)
        assertEquals(
            listOf(
                MusicNewsBlock.Paragraph("Corpo dell'articolo."),
                MusicNewsBlock.Heading("Le date"),
                MusicNewsBlock.Paragraph("Sabato 26 giugno.")
            ),
            article.blocks
        )
        assertNull(MusicNewsArticleParser.jsonLdBody(html, story, "data-missing-marker"))
    }

    @Test
    fun jsonLdBodyStopsAtTheSourceEndMarkerWithoutMetadata() {
        val html = """
            <div class="news-content"><div class="image"><img src="https://www.intermedia.ru/img/lead.jpg"></div>
            <p>Первый абзац.</p><p><em>Цитата</em></p><div class="clear"></div>
            <p>Следующая новость</p>
        """.trimIndent()

        val article = MusicNewsArticleParser.jsonLdBody(html, story, "class=\"news-content\"", "class=\"clear\"")!!

        assertEquals(story.title, article.title)
        assertEquals("", article.author)
        assertEquals(story.imageUrl, article.heroImageUrl)
        assertEquals(
            listOf(MusicNewsBlock.Paragraph("Первый абзац."), MusicNewsBlock.Paragraph("<em>Цитата</em>")),
            article.blocks
        )
    }

    @Test
    fun feedGuidProvidesWordpressPostIdAndSurvivesTheCache() {
        val xml = """
            <rss version="2.0"><channel><item>
              <title>Story</title><link>https://consequence.net/2026/11/story/</link>
              <guid isPermaLink="false">https://consequence.net/?p=1651454</guid>
              <pubDate>Mon, 23 Nov 2026 08:00:00 +0000</pubDate>
            </item></channel></rss>
        """.trimIndent()

        val parsed = MusicNewsFeedParser.parse(xml, "Consequence").single()
        assertEquals("1651454", parsed.articleId)

        val decoded = MusicNewsCodec.decode(MusicNewsCodec.encode(MusicNewsPage(listOf(parsed), 10L)))!!
        assertEquals("1651454", decoded.stories.single().articleId)
        assertEquals("", MusicNewsCodec.decode("""{"fetchedAtMs":1,"stories":[{"url":"https://a.example/1","title":"T"}]}""")!!.stories.single().articleId)
    }

    @Test
    fun articleRequestsTargetEachSourceApi() {
        val wordpress = MusicNewsSources.articleRequest(story)!!
        assertEquals(MusicNewsArticleApi.WordPress, wordpress.api)
        assertTrue(wordpress.url, wordpress.url.startsWith("https://www.rollingstone.it/wp-json/wp/v2/posts/1045820?"))

        val stereogum = MusicNewsSources.articleRequest(
            story.copy(url = "https://stereogum.com/2519772/x/news/", source = "Stereogum", articleId = "2519772")
        )!!
        assertTrue(stereogum.url, stereogum.url.startsWith("https://lede-admin.stereogum.com/wp-json/wp/v2/posts/2519772?"))

        val pitchfork = MusicNewsSources.articleRequest(story.copy(url = "https://pitchfork.com/story/x/", source = "Pitchfork", articleId = ""))!!
        assertEquals(MusicNewsArticleRequest("https://pitchfork.com/story/x/", MusicNewsArticleApi.PreloadedState), pitchfork)

        val rockol = MusicNewsSources.articleRequest(story.copy(url = "https://www.rockol.it/news-762740/x", source = "Rockol", articleId = ""))!!
        assertEquals(MusicNewsArticleApi.JsonLdBody, rockol.api)
        assertEquals("data-rockol-news-detail-body", rockol.bodyMarker)

        assertNull(MusicNewsSources.articleRequest(story.copy(articleId = "")))
        assertNull(MusicNewsSources.articleRequest(story.copy(url = "https://evil.example/wp/1045820/")))
    }
}
