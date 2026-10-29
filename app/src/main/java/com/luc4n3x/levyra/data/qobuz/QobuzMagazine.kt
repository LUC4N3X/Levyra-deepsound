package com.luc4n3x.levyra.data.qobuz

import org.json.JSONArray
import org.json.JSONObject

data class QobuzStory(
    val url: String,
    val title: String,
    val rubric: String,
    val author: String,
    val imageUrl: String
)

data class QobuzRubric(val slug: String, val label: String)

data class QobuzMagazinePage(
    val section: String,
    val featured: List<QobuzStory>,
    val stories: List<QobuzStory>,
    val rubrics: List<QobuzRubric>,
    val fetchedAtMs: Long
) {
    val isEmpty: Boolean get() = featured.isEmpty() && stories.isEmpty()
}

internal object QobuzMagazineParser {
    val SECTIONS = listOf("news", "panoramas", "interviews", "hi-fi")

    private val headerItem = Regex("<div class=\"item magazine-header-item\"(.*?)(?=<div class=\"item magazine-header-item\"|<section|<div class=\"container-cards)", RegexOption.DOT_MATCHES_ALL)
    private val headerTag = Regex("class=\"magazine-header-tag\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val headerLink = Regex("class=\"magazine-header-link\"\\s+href=\"(/[a-z]{2}-[a-z]{2}/magazine/story/[^\"]+)\"")
    private val headerTitle = Regex("class=\"magazine-header-title\"\\s+title=\"([^\"]*)\"")
    private val headerAuthor = Regex("class=\"magazine-header-author\">\\s*<a[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val background = Regex("data-bg=\"url\\((https://[^)\"]+)\\)\"")
    private val card = Regex("<div class=\"section-card [^\"]*\"(.*?)class=\"section-cards-author\">\\s*<a[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val cardLink = Regex("href=\"(/[a-z]{2}-[a-z]{2}/magazine/story/[^\"]+)\"")
    private val cardTag = Regex("class=\"section-cards-tag\"\\s+href=\"[^\"]*\">(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val cardTitle = Regex("<h2 title=\"([^\"]*)\"")
    private val sectionLink = Regex("href=\"/[a-z]{2}-[a-z]{2}/magazine/section/(news|panoramas|interviews|hi-fi)\">\\s*([^<]{2,40}?)\\s*</a>")

    fun parse(section: String, html: String, now: Long): QobuzMagazinePage {
        val featured = headerItem.findAll(html).mapNotNull { match ->
            val body = match.groupValues[1]
            val path = headerLink.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = headerTitle.find(body)?.groupValues?.get(1)?.let(QobuzCatalogParser::text).orEmpty()
            if (title.isBlank()) return@mapNotNull null
            QobuzStory(
                url = STORE + path,
                title = title,
                rubric = headerTag.find(body)?.groupValues?.get(1)?.let(QobuzCatalogParser::text).orEmpty(),
                author = headerAuthor.find(body)?.groupValues?.get(1)?.let(QobuzCatalogParser::text).orEmpty(),
                imageUrl = background.find(body)?.groupValues?.get(1)?.let(::decodeUrl).orEmpty()
            )
        }.distinctBy { it.url }.take(MAX_FEATURED).toList()
        val stories = card.findAll(html).mapNotNull { match ->
            val body = match.groupValues[1]
            val path = cardLink.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = cardTitle.find(body)?.groupValues?.get(1)?.let(QobuzCatalogParser::text).orEmpty()
            if (title.isBlank()) return@mapNotNull null
            QobuzStory(
                url = STORE + path,
                title = title,
                rubric = cardTag.find(body)?.groupValues?.get(1)?.let(QobuzCatalogParser::text).orEmpty(),
                author = QobuzCatalogParser.text(match.groupValues[2]),
                imageUrl = background.find(body)?.groupValues?.get(1)?.let(::decodeUrl).orEmpty()
            )
        }.distinctBy { it.url }.filterNot { story -> featured.any { it.url == story.url } }.take(MAX_STORIES).toList()
        val rubrics = sectionLink.findAll(html)
            .map { QobuzRubric(it.groupValues[1], QobuzCatalogParser.text(it.groupValues[2])) }
            .filter { it.label.isNotBlank() }
            .distinctBy { it.slug }
            .sortedBy { SECTIONS.indexOf(it.slug) }
            .toList()
        return QobuzMagazinePage(section, featured, stories, rubrics, now)
    }

    private fun decodeUrl(value: String): String = value.replace("&amp;", "&")

    private const val STORE = "https://www.qobuz.com"
    private const val MAX_FEATURED = 8
    private const val MAX_STORIES = 30
}

internal object QobuzMagazineCodec {
    private fun stories(list: List<QobuzStory>) = JSONArray().apply {
        list.forEach { put(JSONObject().put("url", it.url).put("title", it.title).put("rubric", it.rubric).put("author", it.author).put("image", it.imageUrl)) }
    }

    private fun stories(array: JSONArray?): List<QobuzStory> = (0 until (array?.length() ?: 0)).mapNotNull { index ->
        val item = array?.optJSONObject(index) ?: return@mapNotNull null
        QobuzStory(item.optString("url"), item.optString("title"), item.optString("rubric"), item.optString("author"), item.optString("image"))
            .takeIf { it.url.startsWith("https://www.qobuz.com/") && it.title.isNotBlank() }
    }

    fun encode(page: QobuzMagazinePage): String = JSONObject()
        .put("section", page.section)
        .put("fetchedAtMs", page.fetchedAtMs)
        .put("featured", stories(page.featured))
        .put("stories", stories(page.stories))
        .put("rubrics", JSONArray().apply { page.rubrics.forEach { put(JSONObject().put("slug", it.slug).put("label", it.label)) } })
        .toString()

    fun decode(raw: String): QobuzMagazinePage? = runCatching {
        val json = JSONObject(raw)
        val rubrics = json.optJSONArray("rubrics") ?: JSONArray()
        QobuzMagazinePage(
            section = json.getString("section"),
            featured = stories(json.optJSONArray("featured")),
            stories = stories(json.optJSONArray("stories")),
            rubrics = (0 until rubrics.length()).mapNotNull { index ->
                rubrics.optJSONObject(index)?.let { QobuzRubric(it.optString("slug"), it.optString("label")) }
                    ?.takeIf { it.slug in QobuzMagazineParser.SECTIONS && it.label.isNotBlank() }
            },
            fetchedAtMs = json.getLong("fetchedAtMs")
        )
    }.getOrNull()
}
