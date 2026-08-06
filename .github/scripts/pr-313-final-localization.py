from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# YouTube Music Samples: every supported language keeps local discovery slots.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeMusicRepository.kt")
text = path.read_text(encoding="utf-8")
old = '''    val localized = when (LevyraLanguageCatalog.normalize(languageCode)) {
        "it" -> listOf("nuovi video musicali italiani", "hit italiane del momento video", "video musicali popolari in Italia")
        "es" -> listOf("nuevos videos musicales españoles", "éxitos latinos del momento video", "videos musicales populares en España")
        "fr" -> listOf("nouveaux clips musicaux français", "tubes français du moment clip", "clips populaires en France")
        "de" -> listOf("neue deutsche musikvideos", "aktuelle deutsche hits musikvideo", "beliebte musikvideos in Deutschland")
        "pt" -> listOf("novos videoclipes brasileiros", "sucessos brasileiros do momento vídeo", "videoclipes populares no Brasil")
        "ja" -> listOf("日本 新着 ミュージックビデオ", "日本 人気曲 公式MV", "J-POP 話題 ミュージックビデオ")
        "ko" -> listOf("한국 신곡 뮤직비디오", "한국 인기곡 공식 뮤직비디오", "K-POP 인기 뮤직비디오")
        else -> listOf("new music videos in my country", "songs popular in my region music video", "local music video hits")
    }.take(YOUTUBE_MUSIC_SAMPLE_LOCALIZED_QUERY_LIMIT)
'''
new = '''    val localized = when (LevyraLanguageCatalog.normalize(languageCode)) {
        "en" -> listOf("new music videos in USA", "songs popular in USA music video", "new English music videos")
        "it" -> listOf("nuovi video musicali italiani", "hit italiane del momento video", "video musicali popolari in Italia")
        "es" -> listOf("nuevos videos musicales españoles", "éxitos latinos del momento video", "videos musicales populares en España")
        "fr" -> listOf("nouveaux clips musicaux français", "tubes français du moment clip", "clips populaires en France")
        "de" -> listOf("neue deutsche musikvideos", "aktuelle deutsche hits musikvideo", "beliebte musikvideos in Deutschland")
        "pt" -> listOf("novos videoclipes brasileiros", "sucessos brasileiros do momento vídeo", "videoclipes populares no Brasil")
        "nl" -> listOf("nieuwe Nederlandse muziekvideo's", "Nederlandse hits van nu video", "populaire muziekvideo's Nederland")
        "pl" -> listOf("nowe polskie teledyski", "polskie hity teraz teledysk", "popularne teledyski Polska")
        "ro" -> listOf("videoclipuri muzicale românești noi", "hituri românești videoclip", "videoclipuri populare România")
        "el" -> listOf("νέα ελληνικά μουσικά βίντεο", "ελληνικές επιτυχίες βίντεο", "δημοφιλή μουσικά βίντεο Ελλάδα")
        "sv" -> listOf("nya svenska musikvideor", "svenska hits just nu video", "populära musikvideor Sverige")
        "da" -> listOf("nye danske musikvideoer", "danske hits lige nu video", "populære musikvideoer Danmark")
        "cs" -> listOf("nové české videoklipy", "české hity právě teď video", "populární videoklipy Česko")
        "uk" -> listOf("нові українські музичні відео", "українські хіти зараз відео", "популярні кліпи Україна")
        "ru" -> listOf("новые русские музыкальные видео", "русские хиты сейчас видео", "популярные клипы Россия")
        "tr" -> listOf("yeni Türk müzik videoları", "güncel Türkçe hitler video", "Türkiye popüler müzik videoları")
        "ar" -> listOf("فيديوهات موسيقية عربية جديدة", "أغاني عربية رائجة فيديو", "فيديوهات موسيقية رائجة عربياً")
        "zh" -> listOf("华语新歌音乐视频", "热门华语歌曲 MV", "华语流行音乐视频")
        "ja" -> listOf("日本 新着 ミュージックビデオ", "日本 人気曲 公式MV", "J-POP 話題 ミュージックビデオ")
        "ko" -> listOf("한국 신곡 뮤직비디오", "한국 인기곡 공식 뮤직비디오", "K-POP 인기 뮤직비디오")
        "hi" -> listOf("नए हिंदी म्यूजिक वीडियो", "भारत के लोकप्रिय गाने वीडियो", "बॉलीवुड नए गाने वीडियो")
        "id" -> listOf("video musik Indonesia terbaru", "lagu Indonesia populer video", "video musik viral Indonesia")
        "vi" -> listOf("video nhạc Việt mới", "bài hát Việt thịnh hành video", "video âm nhạc phổ biến Việt Nam")
        "th" -> listOf("มิวสิกวิดีโอไทยใหม่", "เพลงไทยยอดนิยมวิดีโอ", "มิวสิกวิดีโอยอดนิยมประเทศไทย")
        "fil" -> listOf("bagong Filipino music videos", "OPM hits ngayon video", "sikat na music videos Pilipinas")
        "he" -> listOf("קליפים ישראליים חדשים", "להיטים ישראליים עכשיו וידאו", "קליפים פופולריים בישראל")
        else -> listOf("new music videos in my country", "songs popular in my region music video", "local music video hits")
    }.take(YOUTUBE_MUSIC_SAMPLE_LOCALIZED_QUERY_LIMIT)
'''
text = replace_once(text, old, new, "all-language YouTube Music sample queries")
path.write_text(text, encoding="utf-8")


# NewPipe fallback: reserve local-language queries even for users with a rich profile.
path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsRepository.kt")
text = path.read_text(encoding="utf-8")
old_return = '''    return (followedArtistQueries + seedArtistQueries + songQueries + localizedShortQueries(languageCode))
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(MAX_SHORT_QUERIES)
}
'''
new_return = '''    val personalized = (followedArtistQueries + seedArtistQueries + songQueries)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(2)
    val localized = localizedShortQueries(languageCode)
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinctBy { query -> query.lowercase(Locale.ROOT) }
        .take(2)
    return buildList {
        repeat(maxOf(personalized.size, localized.size)) { index ->
            personalized.getOrNull(index)?.let(::add)
            localized.getOrNull(index)?.let(::add)
        }
    }.take(MAX_SHORT_QUERIES)
}
'''
text = replace_once(text, old_return, new_return, "reserved NewPipe localized query slots")
start = text.index("private fun localizedShortQueries(languageCode: String): List<String> {")
end = text.index("\nprivate fun youtubeVideoId", start)
new_localized = '''private fun localizedShortQueries(languageCode: String): List<String> {
    return when (languageCode.lowercase(Locale.ROOT).substringBefore('-')) {
        "en" -> listOf("music shorts USA", "new songs USA #shorts", "viral English music #shorts", "popular music USA #shorts")
        "it" -> listOf("shorts musica italiana", "canzoni del momento #shorts", "nuove hit italiane #shorts", "musica virale #shorts")
        "es" -> listOf("shorts música española", "canciones del momento #shorts", "éxitos latinos #shorts", "música viral #shorts")
        "fr" -> listOf("shorts musique française", "chansons du moment #shorts", "nouveaux tubes #shorts", "musique virale #shorts")
        "de" -> listOf("shorts deutsche musik", "songs des moments #shorts", "neue hits #shorts", "virale musik #shorts")
        "pt" -> listOf("shorts música brasileira", "músicas do momento #shorts", "novos sucessos #shorts", "música viral #shorts")
        "nl" -> listOf("shorts Nederlandse muziek", "Nederlandse hits #shorts", "nieuwe muziek Nederland #shorts", "virale muziek #shorts")
        "pl" -> listOf("shorts polska muzyka", "polskie hity #shorts", "nowa polska muzyka #shorts", "viral muzyka #shorts")
        "ro" -> listOf("shorts muzică românească", "hituri românești #shorts", "muzică nouă România #shorts", "muzică virală #shorts")
        "el" -> listOf("ελληνική μουσική #shorts", "ελληνικές επιτυχίες #shorts", "νέα ελληνικά τραγούδια #shorts", "viral μουσική #shorts")
        "sv" -> listOf("shorts svensk musik", "svenska hits #shorts", "ny svensk musik #shorts", "viral musik #shorts")
        "da" -> listOf("shorts dansk musik", "danske hits #shorts", "ny dansk musik #shorts", "viral musik #shorts")
        "cs" -> listOf("shorts česká hudba", "české hity #shorts", "nová česká hudba #shorts", "virální hudba #shorts")
        "uk" -> listOf("українська музика #shorts", "українські хіти #shorts", "нові українські пісні #shorts", "вірусна музика #shorts")
        "ru" -> listOf("русская музыка #shorts", "русские хиты #shorts", "новые русские песни #shorts", "вирусная музыка #shorts")
        "tr" -> listOf("Türkçe müzik #shorts", "Türkçe hitler #shorts", "yeni Türkçe şarkılar #shorts", "viral müzik #shorts")
        "ar" -> listOf("موسيقى عربية #shorts", "أغاني عربية رائجة #shorts", "أغاني عربية جديدة #shorts", "موسيقى viral #shorts")
        "zh" -> listOf("华语音乐 #shorts", "华语热门歌曲 #shorts", "华语新歌 #shorts", "热门音乐 #shorts")
        "ja" -> listOf("音楽 #shorts", "新曲 #shorts", "人気曲 #shorts", "j-pop #shorts")
        "ko" -> listOf("음악 #shorts", "신곡 #shorts", "인기곡 #shorts", "k-pop #shorts")
        "hi" -> listOf("हिंदी संगीत #shorts", "नए हिंदी गाने #shorts", "बॉलीवुड हिट्स #shorts", "वायरल संगीत #shorts")
        "id" -> listOf("musik Indonesia #shorts", "lagu Indonesia terbaru #shorts", "hit Indonesia #shorts", "musik viral #shorts")
        "vi" -> listOf("nhạc Việt #shorts", "bài hát Việt mới #shorts", "hit Việt Nam #shorts", "nhạc viral #shorts")
        "th" -> listOf("เพลงไทย #shorts", "เพลงไทยใหม่ #shorts", "เพลงฮิตไทย #shorts", "เพลงไวรัล #shorts")
        "fil" -> listOf("OPM #shorts", "bagong kantang Pilipino #shorts", "Pinoy hits #shorts", "viral music Pilipinas #shorts")
        "he" -> listOf("מוזיקה ישראלית #shorts", "להיטים ישראליים #shorts", "שירים ישראליים חדשים #shorts", "מוזיקה ויראלית #shorts")
        else -> listOf("music #shorts", "songs right now #shorts", "new music #shorts", "viral music #shorts")
    }
}
'''
text = text[:start] + new_localized + text[end:]
path.write_text(text, encoding="utf-8")


# A fresh personalized cache must mark both its language and profile identity.
path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''                if (cached.isFresh()) {
                    musicVideosLoadedLanguage = languageCode
                    return
                }
''',
    '''                if (cached.isFresh()) {
                    musicVideosLoadedLanguage = languageCode
                    musicVideosLoadedProfileSignature = profileSignature
                    return
                }
''',
    "fresh Samples cache profile identity",
)
path.write_text(text, encoding="utf-8")


# Regression coverage for guaranteed personalization + localization.
path = Path("app/src/test/java/com/luc4n3x/levyra/data/YoutubeShortsRepositoryTest.kt")
text = path.read_text(encoding="utf-8")
text = replace_once(
    text,
    '''        assertEquals("Artista seguito #shorts", queries.first())
        assertTrue(queries.contains("Artista ascoltato #shorts"))
        assertTrue(queries.contains("shorts musica italiana"))
''',
    '''        assertEquals("Artista seguito #shorts", queries.first())
        assertTrue(queries.contains("Artista ascoltato #shorts"))
        assertTrue(queries.contains("shorts musica italiana"))
        assertTrue(queries.indexOf("shorts musica italiana") < MAX_TEST_QUERY_BOUND)
''',
    "NewPipe localized slot assertion",
)
text = replace_once(
    text,
    '''class YoutubeShortsRepositoryTest {
''',
    '''class YoutubeShortsRepositoryTest {
    private companion object {
        const val MAX_TEST_QUERY_BOUND = 4
    }

''',
    "NewPipe test bound constant",
)
path.write_text(text, encoding="utf-8")

Path("app/src/test/java/com/luc4n3x/levyra/data/SamplesAllLanguagesPolicyTest.kt").write_text(
    '''package com.luc4n3x.levyra.data

import org.junit.Assert.assertTrue
import org.junit.Test

class SamplesAllLanguagesPolicyTest {
    @Test
    fun everySupportedLanguageProducesLocalizedYoutubeMusicQueries() {
        val representativeTokens = mapOf(
            "it" to "italian",
            "nl" to "Nederland",
            "pl" to "polsk",
            "el" to "ελλην",
            "uk" to "україн",
            "ar" to "عرب",
            "zh" to "华语",
            "hi" to "हिंदी",
            "fil" to "Pilipinas",
            "he" to "ישראל"
        )

        representativeTokens.forEach { (language, token) ->
            val queries = youtubeMusicSampleQueries(emptyList(), emptyList(), language)
            assertTrue("Missing local query for $language", queries.any { it.contains(token, ignoreCase = true) })
        }
    }

    @Test
    fun personalizedNewPipeQueriesNeverRemoveTheLocalMarket() {
        val queries = youtubeShortQueries(
            seeds = List(8) { index -> sample("Song $index", "Artist $index") },
            preferredArtists = List(8) { index -> "Followed $index" },
            languageCode = "it"
        )

        assertTrue(queries.any { it.contains("italiana", ignoreCase = true) })
        assertTrue(queries.any { it.contains("Followed 0", ignoreCase = true) })
    }

    private fun sample(title: String, artist: String) = com.luc4n3x.levyra.domain.Track(
        id = (title + artist).hashCode().toUInt().toString().padStart(11, '0').take(11),
        title = title,
        artist = artist,
        album = title,
        durationMs = 60_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "https://levyra.test/sample.jpg",
        largeThumbnailUrl = "https://levyra.test/sample-large.jpg",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0
    )
}
''',
    encoding="utf-8",
)

print("Completed Samples localization for every supported language")
