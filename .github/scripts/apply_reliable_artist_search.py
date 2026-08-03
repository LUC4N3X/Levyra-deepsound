from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text()
old = '''        val result = runCatching {
            val raw = providerRouter.searchEverything(clean, _state.value.languageCode)
            val officialArtists = artistRepository.officialArtistHits(raw.artists)
            raw.copy(
                artists = officialArtists,
                albums = searchAlbumsForArtistQuery(clean, raw, officialArtists)
            )
        }
'''
new = '''        val result = runCatching {
            coroutineScope {
                val rawSearch = async {
                    providerRouter.searchEverything(clean, _state.value.languageCode)
                }
                val exactArtistSearch = async {
                    runCatching { artistRepository.artistHitFor(clean) }.getOrNull()
                }
                val raw = rawSearch.await()
                val exactArtist = exactArtistSearch.await()
                val generalCandidates = raw.artists.filterNot { candidate ->
                    exactArtist != null &&
                        candidate.browseId.isNotBlank() &&
                        candidate.browseId.equals(exactArtist.browseId, ignoreCase = true)
                }
                val verifiedGeneralArtists = artistRepository.officialArtistHits(generalCandidates)
                val officialArtists = mergeReliableArtistSearchResults(
                    query = clean,
                    exactArtist = exactArtist,
                    verifiedArtists = verifiedGeneralArtists
                )
                raw.copy(
                    artists = officialArtists,
                    albums = searchAlbumsForArtistQuery(clean, raw, officialArtists)
                )
            }
        }
'''
count = text.count(old)
if count != 1:
    raise SystemExit(f"Expected one runSearch block, found {count}")
path.write_text(text.replace(old, new))
