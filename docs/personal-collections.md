# Personal collections

Levyra can import a personal audio collection from an M3U/M3U8 or JSON file.
The source can be selected from device storage or fetched from an HTTP or HTTPS
URL. Imported tracks remain separate from provider-backed catalog tracks and
are played directly from their declared URLs or from an offline copy.

## JSON format

The recommended form is an object with collection metadata and a `tracks`
array:

```json
{
  "name": "My collection",
  "baseUrl": "https://media.example.com/audio/",
  "artwork": "https://media.example.com/collection.jpg",
  "tracks": [
    {
      "id": "track-001",
      "title": "First light",
      "artist": "Example Artist",
      "album": "Dawn",
      "albumArtist": "Example Artist",
      "duration": 213.4,
      "url": "first-light.flac",
      "artwork": "covers/dawn.jpg",
      "trackNumber": 1,
      "discNumber": 1,
      "year": 2026
    },
    {
      "title": "Night drive",
      "artists": ["Example Artist", "Guest Artist"],
      "durationMs": 198000,
      "audioUrl": "https://cdn.example.net/night-drive.mp3"
    }
  ]
}
```

The top-level array may also be named `items`. Collection artwork accepts
`artwork`, `cover`, or `image`. Relative audio, artwork, and video URLs resolve
against `baseUrl`; when `baseUrl` is omitted, they resolve against the URL of
the imported JSON document. A file imported from device storage therefore
needs an absolute `baseUrl` for relative URLs.

Each track accepts these fields:

| Purpose | Accepted fields | Notes |
|:---|:---|:---|
| Stable identity | `id`, `guid` | Optional. Keep it unchanged across refreshes. |
| Title | `title` | Falls back to the audio filename. |
| Artist | `artist`, `artists` | `artists` may be a string or an array of strings. |
| Album | `album` | Optional. |
| Album artist | `albumArtist` | Optional. |
| Duration | `duration`, `durationMs` | `duration` is in seconds; `durationMs` takes precedence. |
| Audio URL | `url`, `audioUrl`, `streamUrl`, `src`, `file` | Required; only HTTP and HTTPS are accepted. |
| Artwork | `artwork`, `artworkUrl`, `cover`, `image`, `thumbnail` | Optional absolute or relative URL. |
| Video URL | `videoUrl`, `video` | Stored for compatibility but not played by personal collections. |
| Position | `trackNumber`, `discNumber` | Optional non-negative integers. |
| Year | `year` | Optional integer from 1 to 9999. |

A JSON document may instead be a simple array. Each element can be a complete
track object or an audio URL string:

```json
[
  "https://media.example.com/audio/one.mp3",
  {
    "title": "Two",
    "url": "https://media.example.com/audio/two.ogg"
  }
]
```

## M3U and M3U8 format

Levyra accepts plain URL lists and extended M3U metadata:

```m3u
#EXTM3U
#PLAYLIST:My collection
#EXTIMG:https://media.example.com/collection.jpg
#EXTINF:213 tvg-logo="https://media.example.com/dawn.jpg" album="Dawn",Example Artist - First light
https://media.example.com/audio/first-light.flac
#EXTALB:Night sessions
#EXTART:Example Artist
#EXTINF:198,Example Artist - Night drive
https://media.example.com/audio/night-drive.mp3
```

Supported directives are:

- `#PLAYLIST` for the collection name;
- `#EXTINF` for duration, display title, artist, `album`, and artwork attributes
  named `tvg-logo`, `logo`, or `artwork`;
- `#EXTALB` and `#EXTART` for the next track;
- `#EXTIMG` for collection artwork before the first entry, or artwork for the
  next track after entries begin.

In an `#EXTINF` display value, `Artist - Title` is split into artist and title.
Relative URLs resolve against the source document URL. M3U files selected from
device storage should therefore use absolute HTTP or HTTPS URLs.

M3U8 describes the text encoding here; it does not make a collection an HLS
manifest. Levyra rejects playlists containing HLS manifest directives such as
`#EXT-X-STREAM-INF` or `#EXT-X-TARGETDURATION`. An individual track URL may
point to an HLS stream, but HLS and DASH tracks cannot be saved for offline use.

## Limits and normalization

- The source document is limited to 8 MiB and 20,000 accepted tracks.
- URLs are limited to 4,096 characters. Text metadata and declared IDs are
  limited to 512 characters.
- Durations must be greater than zero and shorter than 24 hours.
- Only HTTP and HTTPS audio, artwork, and video URLs are accepted.
- Duplicate identities are ignored. A declared JSON `id` or `guid` defines the
  identity; otherwise Levyra derives it from the normalized audio URL.
- UTF-8, UTF-8 with BOM, UTF-16 LE/BE with BOM, and Windows-1252 fallback are
  supported.
- Refresh preserves the original add time and any completed offline copy for a
  matching stable track identity.

## Network and credential safety

HTTPS is the default. Each collection requires an explicit opt-in before Levyra
uses cleartext HTTP. On supported Android versions, access to private LAN hosts
also requires the Local Network permission. Loopback, link-local, and multicast
addresses are always rejected.

Basic authentication can be entered during import. Credentials embedded in a
URL are removed from the stored and displayed URL, encrypted with Android
Keystore, and sent only to their matching origin. Redirects to another origin
do not receive the authorization header. Levyra also removes cookies,
`Referer`, and `Origin` from collection requests.

Do not publish collection files containing real credentials. Prefer the import
screen's username and password fields over credentials embedded in a URL.

## Current limitations

- `videoUrl` is retained but personal collections currently use audio mode only.
- Cast receivers cannot access collection credentials stored on the Android
  device.
- Revoking HTTP consent does not remove audio already present in the local
  Media3 cache.
- Lyrics lookup may use the track title and artist like a local file; the source
  URL is not sent to lyrics providers.
- Removing a collection or removing a track during refresh does not delete a
  completed MediaStore download. Offline copies are managed separately.
