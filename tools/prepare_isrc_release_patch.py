from pathlib import Path
import re

path = Path("tools/apply_isrc_release_youtube_auth.py")
text = path.read_text()

replacements = [
    (
        '''models = replace_once(
    models,
    '    val explicit: Boolean = false\\n)\\n\\n@Immutable\\ndata class ArtistProfile(',
    '    val explicit: Boolean = false,\\n    val releaseType: ReleaseType = ReleaseType.Unknown\\n)\\n\\n@Immutable\\ndata class ArtistProfile(',
    'artist release type'
)
models = replace_once(
    models,
    '    val videosParams: String = ""\\n)\\n\\n@Immutable\\ndata class SearchResults(',
    '    val videosParams: String = "",\\n    val compilations: List<ArtistRelease> = emptyList()\\n)\\n\\n@Immutable\\ndata class SearchResults(',
    'artist compilations'
)''',
        '''models = replace_once(
    models,
    '    val playlistId: String = "",\\n    val explicit: Boolean = false\\n)',
    '    val playlistId: String = "",\\n    val explicit: Boolean = false,\\n    val releaseType: ReleaseType = ReleaseType.Unknown\\n)',
    'artist release type'
)
models = replace_once(
    models,
    '    val videosBrowseId: String = "",\\n    val videosParams: String = ""\\n) {',
    '    val videosBrowseId: String = "",\\n    val videosParams: String = "",\\n    val compilations: List<ArtistRelease> = emptyList()\\n) {',
    'artist compilations'
)''',
        "model patch block",
    ),
    (
        '''models = replace_once(
    models,
    '    val metadataConfidence: Int = 0\\n)\\n\\ndata class AlbumDetail(',
    '    val metadataConfidence: Int = 0,\\n    val releaseType: ReleaseType = ReleaseType.Unknown\\n)\\n\\ndata class AlbumDetail(',
    'album hit release type'
)''',
        '''models = replace_once(
    models,
    '    val metadataProvider: String = "",\\n    val metadataConfidence: Int = 0\\n)',
    '    val metadataProvider: String = "",\\n    val metadataConfidence: Int = 0,\\n    val releaseType: ReleaseType = ReleaseType.Unknown\\n)',
    'album hit release type'
)''',
        "album model patch block",
    ),
    (
        '''vm = replace_once(
    vm,
    '    private val _state = MutableStateFlow(LevyraUiState())\\n',
    '    private val _state = MutableStateFlow(LevyraUiState(youtubeMusicAuthenticated = repository.hasYoutubeMusicSession()))\\n',
    'viewmodel initial auth state'
)''',
        '''vm = replace_once(
    vm,
    '            playbackDiagnostics = resolver.playbackDiagnostics()\\n',
    '            playbackDiagnostics = resolver.playbackDiagnostics(),\\n            youtubeMusicAuthenticated = repository.hasYoutubeMusicSession()\\n',
    'viewmodel initial auth state'
)''',
        "viewmodel state patch block",
    ),
    (
        '''artwork = replace_once(
    artwork,
    '        val referenceIsrc = normalizeIdentifier(track.isrc)\\n        val candidateIsrc = normalizeIdentifier(candidate.isrc)\\n',
    ''' + "'''" + '''        val referenceIsrc = normalizeIdentifier(track.isrc)
        val candidateIsrc = normalizeIdentifier(candidate.isrc)
        when (recordingIdentityMatch(referenceIsrc, candidateIsrc)) {
            RecordingIdentityMatch.Exact -> return 10_000
            RecordingIdentityMatch.Conflict -> return Int.MIN_VALUE
            RecordingIdentityMatch.Unknown -> Unit
        }
''' + "'''" + ''',
    'artwork isrc gate'
)''',
        '''artwork = replace_once(
    artwork,
    '        val exactIsrc = track.isrc.isNotBlank() && isrc.isNotBlank() && track.isrc.equals(isrc, true)\\n',
    ''' + "'''" + '''        when (recordingIdentityMatch(track.isrc, isrc)) {
            RecordingIdentityMatch.Exact -> return 10_000
            RecordingIdentityMatch.Conflict -> return REJECTED_SCORE
            RecordingIdentityMatch.Unknown -> Unit
        }
''' + "'''" + ''',
    'artwork isrc gate'
)
artwork = replace_once(
    artwork,
    '        if (!exactIsrc && artistScore < MIN_ARTIST_MATCH_SCORE) return REJECTED_SCORE\\n',
    '        if (artistScore < MIN_ARTIST_MATCH_SCORE) return REJECTED_SCORE\\n',
    'artwork artist gate'
)
artwork = replace_once(
    artwork,
    '        if (track.isrc.isNotBlank() && isrc.isNotBlank()) score += if (exactIsrc) 220 else -100\\n',
    '',
    'remove legacy ISRC score'
)''',
        "artwork patch block",
    ),
]

for old, new, label in replacements:
    if old not in text:
        raise SystemExit(f"{label} not found")
    text = text.replace(old, new, 1)

helper = '''def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    count = text.count(old)
    if count != 1:
        if label in {'two row release classifier', 'viewmodel initial auth state'} and count == 2:
            return text.replace(old, new, 1)
        raise SystemExit(f'{label}: expected one anchor, found {count}')
    return text.replace(old, new, 1)


def regex_once'''
text, count = re.subn(
    r"def replace_once\(text: str, old: str, new: str, label: str\) -> str:\n.*?\n\n\ndef regex_once",
    lambda _: helper,
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit("replace helper function not found")

path.write_text(text)
