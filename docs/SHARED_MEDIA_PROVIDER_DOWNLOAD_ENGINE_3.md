# Shared Media, Provider Router, and Download Engine 3.0

## Shared media

Levyra accepts shared links via Android's `ACTION_SEND`, `ACTION_SEND_MULTIPLE`, and standard web URLs for YouTube and YouTube Music.

Supported targets:

- Standard videos, Shorts, live streams, and shortened links
- Public and unlisted playlists
- YouTube Music albums via browse IDs
- Artists, channels, and user handles
- Plain shared text (opened as an in-app search query)

The incoming intent is parsed, normalized, and resolved before displaying the preview dialog. From the preview, you can start playback immediately, insert the track next, add it to the end of the queue, or download it for offline use.

## Provider router

Catalog queries and stream resolution pass through modular provider interfaces.

The router handles:

- Deterministic provider ordering
- Latency tracking across endpoints
- Failure counters and timeout enforcement
- Automated fallback execution
- Circuit breaking after repeated failures
- Diagnostics accessible in the troubleshooting report

Default catalog providers include YouTube Music and Levyra's local library index. Playback relies on the stream cache first, falling back to Levyra's native resolver.

To add a provider, implement `LevyraCatalogProvider` or `LevyraPlaybackProvider` and register it in `LevyraViewModel`.

## Download Engine 3.0

Download options include:

- Automatic, High Quality, and Data Saver presets
- Independent offline bitrate selection
- Flat, Artist, and Artist/Album folder organization
- Configurable bandwidth throttling
- Wi-Fi and charging constraints
- Resumable file downloads
- Support for 1 to 4 concurrent download jobs
- Embedded metadata, album artwork, and synced lyrics
- Container integrity verification
- Automatic detection and reuse of existing complete files

The High Quality preset fetches a high-bitrate offline stream without changing your active streaming setting. Data Saver requests a lower-bandwidth stream and limits transfer speed. Automatic mirrors your playback preferences and reuses an active stream when possible.

The download engine maintains parallel chunk downloads, sequential fallback, bounded retry logic, storage space checks, MediaStore integration, foreground notifications, and Room database tracking.
