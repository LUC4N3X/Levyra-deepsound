from pathlib import Path

# 1) Reliable artist search: near-exact typo handling + authoritative winner.
artist_file = Path('app/src/main/java/com/luc4n3x/levyra/viewmodel/ReliableArtistSearch.kt')
s = artist_file.read_text(encoding='utf-8')

old = 'import com.luc4n3x.levyra.domain.artistIdentityKey\nimport com.luc4n3x.levyra.domain.artistIdentityMatches\n'
new = 'import com.luc4n3x.levyra.domain.artistIdentityKey\nimport com.luc4n3x.levyra.domain.artistIdentityKeys\nimport com.luc4n3x.levyra.domain.artistIdentityMatches\n'
assert s.count(old) == 1
s = s.replace(old, new, 1)

old = '''    val candidates = (listOfNotNull(exactArtist) + verifiedArtists)\n    val queryMatches = candidates.filter { artistIdentityMatches(it.name, query) }\n    queryMatches.maxWithOrNull(artistAuthorityOrder)?.let(::add)\n\n    verifiedArtists\n        .sortedWith(\n            compareByDescending<ArtistHit> { artistIdentityMatches(it.name, query) }\n                .then(artistAuthorityOrder.reversed())\n                .thenBy { it.name.lowercase() }\n        )\n        .forEach(::add)\n\n    return merged.values.take(limit.coerceIn(1, 24))\n}\n\n/**\n * Ranks artists that match the query equally well: a verified official page and a real audience\n * beat a same-named channel with a handful of subscribers.\n */\n'''
new = '''    val candidates = (listOfNotNull(exactArtist) + verifiedArtists)\n    val typoDistance = allowedArtistTypoDistance(query)\n    val nearExactMatches = candidates.filter { candidate ->\n        artistNameEditDistance(query, candidate.name) <= typoDistance\n    }\n    if (nearExactMatches.isNotEmpty()) {\n        nearExactMatches.maxWithOrNull(artistAuthorityOrder)?.let(::add)\n        return merged.values.take(limit.coerceIn(1, 24))\n    }\n\n    verifiedArtists\n        .sortedWith(\n            compareByDescending<ArtistHit> { artistIdentityMatches(it.name, query) }\n                .then(artistAuthorityOrder.reversed())\n                .thenBy { it.name.lowercase() }\n        )\n        .forEach(::add)\n\n    return merged.values.take(limit.coerceIn(1, 24))\n}\n\nprivate fun allowedArtistTypoDistance(query: String): Int {\n    val shortestKey = artistIdentityKeys(query).minOfOrNull(String::length) ?: return 0\n    return when {\n        shortestKey >= 12 -> 2\n        shortestKey >= 6 -> 1\n        else -> 0\n    }\n}\n\nprivate fun artistNameEditDistance(query: String, candidate: String): Int {\n    val queryKeys = artistIdentityKeys(query)\n    val candidateKeys = artistIdentityKeys(candidate)\n    if (queryKeys.isEmpty() || candidateKeys.isEmpty()) return Int.MAX_VALUE\n    return queryKeys.minOf { queryKey ->\n        candidateKeys.minOf { candidateKey -> levenshteinDistance(queryKey, candidateKey) }\n    }\n}\n\nprivate fun levenshteinDistance(first: String, second: String): Int {\n    if (first == second) return 0\n    if (first.isEmpty()) return second.length\n    if (second.isEmpty()) return first.length\n\n    var previous = IntArray(second.length + 1) { it }\n    first.forEachIndexed { firstIndex, firstChar ->\n        val current = IntArray(second.length + 1)\n        current[0] = firstIndex + 1\n        second.forEachIndexed { secondIndex, secondChar ->\n            val substitution = previous[secondIndex] + if (firstChar == secondChar) 0 else 1\n            current[secondIndex + 1] = minOf(\n                current[secondIndex] + 1,\n                previous[secondIndex + 1] + 1,\n                substitution\n            )\n        }\n        previous = current\n    }\n    return previous[second.length]\n}\n\n/**\n * Ranks artists that match the query equally well: a verified official page and a real audience\n * beat a same-named channel with a handful of subscribers.\n */\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)
artist_file.write_text(s, encoding='utf-8')

# 2) Search identity: exclude top-result recordings from Songs shelf by id OR reliable metadata key.
identity_file = Path('app/src/main/java/com/luc4n3x/levyra/data/SearchEntityIdentity.kt')
s = identity_file.read_text(encoding='utf-8')
anchor = '''    }.take(boundedLimit)\n}\n\ninternal fun mergeSearchAlbums(existing: List<AlbumHit>, incoming: List<AlbumHit>): List<AlbumHit> =\n'''
replacement = '''    }.take(boundedLimit)\n}\n\ninternal fun filterSearchSongsExcludingTopResult(\n    songs: List<Track>,\n    topResultTracks: List<Track>\n): List<Track> {\n    if (songs.isEmpty() || topResultTracks.isEmpty()) return songs\n\n    val topResultIds = topResultTracks\n        .mapNotNullTo(HashSet()) { track -> searchSongIdentityKey(track).takeIf(String::isNotBlank) }\n    val topResultMetadataKeys = topResultTracks\n        .mapNotNullTo(HashSet()) { track -> searchSongMetadataKey(track).takeIf(String::isNotBlank) }\n\n    return songs.filterNot { song ->\n        val identity = searchSongIdentityKey(song)\n        val metadata = searchSongMetadataKey(song)\n        identity in topResultIds || (metadata.isNotBlank() && metadata in topResultMetadataKeys)\n    }\n}\n\ninternal fun mergeSearchAlbums(existing: List<AlbumHit>, incoming: List<AlbumHit>): List<AlbumHit> =\n'''
assert s.count(anchor) == 1
s = s.replace(anchor, replacement, 1)
identity_file.write_text(s, encoding='utf-8')

# 3) UI: use the tested helper instead of id-only filtering.
ui_file = Path('app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt')
u = ui_file.read_text(encoding='utf-8')
old = 'import com.luc4n3x.levyra.data.albumRecommendationDeduplicationKey\nimport com.luc4n3x.levyra.data.selectSearchTopResultTracks\n'
new = 'import com.luc4n3x.levyra.data.albumRecommendationDeduplicationKey\nimport com.luc4n3x.levyra.data.filterSearchSongsExcludingTopResult\nimport com.luc4n3x.levyra.data.selectSearchTopResultTracks\n'
assert u.count(old) == 1
u = u.replace(old, new, 1)
old = '''                            val songs = if (filter == SearchFilter.All) {\n                                val topResultIds = topResultTracks.mapTo(HashSet()) { it.id }\n                                data.songs.filterNot { it.id in topResultIds }\n                            } else {\n                                data.songs\n                            }\n'''
new = '''                            val songs = if (filter == SearchFilter.All) {\n                                filterSearchSongsExcludingTopResult(data.songs, topResultTracks)\n                            } else {\n                                data.songs\n                            }\n'''
assert u.count(old) == 1
u = u.replace(old, new, 1)
ui_file.write_text(u, encoding='utf-8')

# 4) Regression tests: artist typo/authority and top-result shelf duplicate.
artist_test = Path('app/src/test/java/com/luc4n3x/levyra/viewmodel/ReliableArtistSearchTest.kt')
t = artist_test.read_text(encoding='utf-8')
anchor = '''    @Test\n    fun tinyHomonymIsNotPropagatedToSongCredits() {\n'''
addition = '''    @Test\n    fun nearExactMisspellingKeepsOnlyAuthoritativeArtist() {\n        val official = artist(\n            "The Weeknd",\n            "UC-official",\n            subscribers = "Artist · 274M monthly audience"\n        )\n        val tinyHomonym = artist(\n            "The weeknd",\n            "UC-tiny",\n            subscribers = "Artist · 7 subscribers"\n        )\n        val nearbyName = artist(\n            "The Weekend Dreamers",\n            "UC-dreamers",\n            subscribers = "Artist · 20K subscribers"\n        )\n\n        val result = mergeReliableArtistSearchResults(\n            query = "the weekend",\n            exactArtist = tinyHomonym,\n            verifiedArtists = listOf(tinyHomonym, nearbyName, official)\n        )\n\n        assertEquals(listOf("UC-official"), result.map { it.browseId })\n    }\n\n'''
assert t.count(anchor) == 1
t = t.replace(anchor, addition + anchor, 1)
artist_test.write_text(t, encoding='utf-8')

identity_test = Path('app/src/test/java/com/luc4n3x/levyra/data/SearchEntityIdentityTest.kt')
t = identity_test.read_text(encoding='utf-8')
anchor = '''    @Test\n    fun `top result does not invent related tracks when hero artist is missing`() {\n'''
addition = '''    @Test\n    fun `songs shelf excludes metadata duplicate of top result`() {\n        val hero = track(\n            id = "hero-video",\n            title = "Blinding Lights",\n            artist = "The Weeknd",\n            durationMs = 200_000L\n        )\n        val alternateVideo = track(\n            id = "alternate-video",\n            title = "Blinding Lights",\n            artist = "The Weeknd",\n            durationMs = 200_000L\n        )\n        val other = track(\n            id = "other",\n            title = "Save Your Tears",\n            artist = "The Weeknd",\n            durationMs = 215_000L\n        )\n\n        val filtered = filterSearchSongsExcludingTopResult(\n            songs = listOf(alternateVideo, other),\n            topResultTracks = listOf(hero)\n        )\n\n        assertEquals(listOf("other"), filtered.map { it.id })\n    }\n\n    @Test\n    fun `songs shelf keeps alternate id when recording metadata is incomplete`() {\n        val hero = track(id = "hero-video", title = "Unknown", artist = "Artist", durationMs = 0L)\n        val alternateVideo = track(id = "alternate-video", title = "Unknown", artist = "Artist", durationMs = 0L)\n\n        val filtered = filterSearchSongsExcludingTopResult(\n            songs = listOf(alternateVideo),\n            topResultTracks = listOf(hero)\n        )\n\n        assertEquals(listOf("alternate-video"), filtered.map { it.id })\n    }\n\n'''
assert t.count(anchor) == 1
t = t.replace(anchor, addition + anchor, 1)
identity_test.write_text(t, encoding='utf-8')
