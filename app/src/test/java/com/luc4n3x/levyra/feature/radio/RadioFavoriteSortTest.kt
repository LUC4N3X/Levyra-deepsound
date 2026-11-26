package com.luc4n3x.levyra.feature.radio

import com.luc4n3x.levyra.ui.i18n.radioFavoriteSortCopy
import com.luc4n3x.levyra.ui.i18n.radioFavoriteSortLocalizationCodes
import com.luc4n3x.levyra.ui.i18n.supportedLocalizationCodes
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioFavoriteSortTest {
    private val labels = RadioFavoriteGroupLabels(
        countryName = { code -> Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH) },
        genreName = { it.name },
        unknownCountry = "Unknown country",
        otherGenre = "Other"
    )

    @Test
    fun defaultKeepsTheSavedNewestFirstOrder() {
        val favorites = listOf(station("c", "Zeta", addedAt = 30), station("a", "alpha", addedAt = 20), station("b", "Beta", addedAt = 10))
        assertEquals(RadioFavoriteSort.Newest, RadioFavoriteSort.Default)
        assertEquals(listOf("c", "a", "b"), ids(RadioFavoriteSort.Default, favorites))
        assertEquals(listOf("b", "a", "c"), ids(RadioFavoriteSort.Oldest, favorites))
    }

    @Test
    fun nameSortIgnoresCaseAndAccentsAndBreaksTiesByPosition() {
        val favorites = listOf(
            station("1", "zulu"), station("2", "Élan"), station("3", "alpha"), station("4", "Alpha"), station("5", "Bravo")
        )
        assertEquals(listOf("3", "4", "5", "2", "1"), ids(RadioFavoriteSort.NameAscending, favorites))
        assertEquals(listOf("1", "2", "5", "4", "3"), ids(RadioFavoriteSort.NameDescending, favorites))
    }

    @Test
    fun countryGroupsUseLocalizedNamesAndPutUnknownLast() {
        val favorites = listOf(
            station("it-2", "Radio Zeta", countryCode = "IT"),
            station("none", "Nowhere"),
            station("fr", "France Inter", countryCode = "FR"),
            station("it-1", "Radio Alfa", countryCode = "it"),
            station("named", "Named Only", country = "Atlantis")
        )
        val groups = radioFavoriteGroups(favorites, RadioFavoriteSort.Country, Locale.ENGLISH, labels, ::facts)
        assertEquals(listOf("Atlantis", "France", "Italy", "Unknown country"), groups.map { it.title })
        assertEquals(listOf("it-1", "it-2"), groups[2].items.map { it.uuid })
        assertEquals(listOf("none"), groups.last().items.map { it.uuid })
        assertEquals(groups.map { it.key }.distinct(), groups.map { it.key })
    }

    @Test
    fun genreGroupsUseTheFirstRecognizedTagAndPutOtherLast() {
        val favorites = listOf(
            station("pop-rock", "B", tags = listOf("top 40", "Pop Rock")),
            station("rock-pop", "A", tags = listOf("rock pop")),
            station("hiphop", "C", tags = listOf("Hip-Hop", "news")),
            station("untagged", "D"),
            station("unknown-tags", "E", tags = listOf("italia", "90s")),
            station("classic-rock", "F", tags = listOf("classic rock"))
        )
        val groups = radioFavoriteGroups(favorites, RadioFavoriteSort.Genre, Locale.ENGLISH, labels, ::facts)
        assertEquals(listOf("HipHop", "Pop", "Rock", "Other"), groups.map { it.title })
        assertEquals(listOf("pop-rock"), groups[1].items.map { it.uuid })
        assertEquals(listOf("rock-pop", "classic-rock"), groups[2].items.map { it.uuid })
        assertEquals(listOf("untagged", "unknown-tags"), groups.last().items.map { it.uuid })
    }

    @Test
    fun primaryGenreMatchesWholeWordsOnly() {
        assertEquals(RadioCategory.Electronic, primaryRadioGenre(listOf("deep house")))
        assertEquals(RadioCategory.Classical, primaryRadioGenre(listOf("Clàssical")))
        assertNull(primaryRadioGenre(listOf("warehouse", "popular", "")))
        assertNull(primaryRadioGenre(emptyList()))
    }

    @Test
    fun ungroupedSortsHaveNoHeaderAndEmptyFavoritesHaveNoGroups() {
        val favorites = listOf(station("a", "A"))
        RadioFavoriteSort.entries.filter { it != RadioFavoriteSort.Country && it != RadioFavoriteSort.Genre }.forEach { sort ->
            assertNull(radioFavoriteGroups(favorites, sort, Locale.ENGLISH, labels, ::facts).single().title)
        }
        RadioFavoriteSort.entries.forEach { sort ->
            assertTrue(radioFavoriteGroups(emptyList<RadioStation>(), sort, Locale.ENGLISH, labels, ::facts).isEmpty())
        }
    }

    @Test
    fun persistedIdsAreStableAndUnknownIdsFallBackToDefault() {
        assertEquals(listOf("newest", "oldest", "name_asc", "name_desc", "country", "genre"), RadioFavoriteSort.entries.map { it.id })
        RadioFavoriteSort.entries.forEach { assertEquals(it, RadioFavoriteSort.fromId(it.id)) }
        assertEquals(RadioFavoriteSort.Default, RadioFavoriteSort.fromId(null))
        assertEquals(RadioFavoriteSort.Default, RadioFavoriteSort.fromId("NameAscending"))
    }

    @Test
    fun everySupportedLanguageHasCompleteSortCopy() {
        assertEquals(supportedLocalizationCodes() + "pt-BR", radioFavoriteSortLocalizationCodes())
        radioFavoriteSortLocalizationCodes().forEach { code ->
            val copy = radioFavoriteSortCopy(code)
            val values = listOf(copy.sortBy, copy.otherGenre, copy.unknownCountry) + RadioFavoriteSort.entries.map(copy::label)
            assertTrue(code, values.all(String::isNotBlank))
            assertEquals(code, RadioFavoriteSort.entries.size, RadioFavoriteSort.entries.map(copy::label).distinct().size)
        }
        assertEquals("Gênero", radioFavoriteSortCopy("pt-BR").genre)
        assertEquals("Paese sconosciuto", radioFavoriteSortCopy("it").unknownCountry)
    }

    private fun ids(sort: RadioFavoriteSort, favorites: List<RadioStation>) =
        radioFavoriteGroups(favorites, sort, Locale.ENGLISH, labels, ::facts).flatMap { group -> group.items.map { it.uuid } }

    private fun facts(station: RadioStation) =
        RadioFavoriteFacts(station.name, station.countryCode, station.country, station.tags, station.addedAt)

    private fun station(
        uuid: String,
        name: String,
        countryCode: String = "",
        country: String = "",
        tags: List<String> = emptyList(),
        addedAt: Long = 0L
    ) = RadioStation(
        uuid = uuid, name = name, streamUrl = "https://radio.example/$uuid", resolvedStreamUrl = "", faviconUrl = "",
        homepageUrl = "", country = country, countryCode = countryCode, language = "", tags = tags, codec = "",
        bitrateKbps = 0, votes = 0, clickCount = 0, lastCheckOk = true, addedAt = addedAt
    )
}
