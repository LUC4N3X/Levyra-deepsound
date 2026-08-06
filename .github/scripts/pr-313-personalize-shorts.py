from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")
old = '''            val seedSnapshot = _state.value
            val seeds = buildList {
                addAll(seedSnapshot.exploreTracks)
                addAll(seedSnapshot.charts)
                seedSnapshot.homeSections.forEach { section -> addAll(section.tracks) }
                addAll(seedSnapshot.tracks)
            }
                .distinctBy { track -> track.id }
                .take(32)

            val feedResult = try {
                shortsRepository.feed(
                    seeds = seeds,
                    languageCode = languageCode,
                    limit = EXPLORE_SHORTS_FEED_LIMIT
                )
'''
new = '''            val seedSnapshot = _state.value
            val seeds = buildList {
                seedSnapshot.currentTrack?.let { track -> add(track) }
                addAll(seedSnapshot.recentListens)
                addAll(seedSnapshot.favorites)
                addAll(seedSnapshot.personalOrbitTracks)
                addAll(seedSnapshot.homeResonanceTracks)
                addAll(seedSnapshot.exploreTracks)
                addAll(seedSnapshot.charts)
                seedSnapshot.homeSections.forEach { section -> addAll(section.tracks) }
                addAll(seedSnapshot.tracks)
            }
                .distinctBy { track -> track.id }
                .take(48)
            val preferredArtists = buildList {
                addAll(seedSnapshot.followedArtists.map { artist -> artist.name })
                addAll(seedSnapshot.recentListens.map { track -> track.artist })
                addAll(seedSnapshot.favorites.map { track -> track.artist })
                addAll(seedSnapshot.personalOrbitTracks.map { track -> track.artist })
                addAll(seeds.map { track -> track.artist })
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinctBy { artist -> artist.lowercase(java.util.Locale.ROOT) }
                .take(16)
            val preferredChannelIds = buildList {
                addAll(seedSnapshot.followedArtists.map { artist -> artist.browseId })
                seeds.forEach { track -> addAll(track.artistBrowseIds) }
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .take(20)

            val feedResult = try {
                shortsRepository.feed(
                    seeds = seeds,
                    languageCode = languageCode,
                    preferredArtists = preferredArtists,
                    preferredChannelIds = preferredChannelIds,
                    limit = EXPLORE_SHORTS_FEED_LIMIT
                )
'''
text = replace_once(text, old, new, "personalized Shorts seed block")
path.write_text(text, encoding="utf-8")
print("Applied language- and preference-aware Shorts seed selection")
