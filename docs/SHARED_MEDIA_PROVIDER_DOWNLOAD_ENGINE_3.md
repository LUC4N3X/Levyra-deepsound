# Levyra Shared Media, Provider Router and Download Engine 3.0

## Shared media

Levyra accepts Android `ACTION_SEND`, `ACTION_SEND_MULTIPLE` and browsable YouTube or YouTube Music links.

Supported targets:

- videos, Shorts, live and shortened links;
- playlists;
- YouTube Music albums through their browse ID;
- artists, channels and handles;
- plain shared text as a Levyra search.

The launch intent is parsed once, normalized and resolved before showing the preview. The preview can start playback, insert after the current track, append to the queue or enqueue an offline export.

## Provider router

Catalog and playback access now pass through independent provider contracts.

The router provides:

- deterministic provider priority;
- latency tracking;
- failure counters;
- timeouts;
- fallback execution;
- circuit breaking after repeated failures;
- diagnostics exposed through the existing diagnostic report.

The initial catalog providers are YouTube Music and Levyra's local in-memory catalog. Playback uses the resolver cache first and Levyra's native resolver second.

A new provider must implement `LevyraCatalogProvider` or `LevyraPlaybackProvider` and be registered in `LevyraViewModel`.

## Download Engine 3.0

Download settings now include:

- Automatic, High Quality and Data Saver presets;
- independent offline stream quality selection;
- Flat, Artist and Artist/Album folder layouts;
- configurable bandwidth limits;
- Wi-Fi and charging constraints;
- resumable downloads;
- one to four simultaneous jobs;
- optional metadata and artwork embedding;
- container verification;
- reuse of valid existing downloads.

High Quality requests a fresh high-quality offline stream without changing active playback quality. Data Saver requests a low-quality stream and applies a default network limit. Automatic keeps the current playback preference and can reuse a valid stream already attached to the track.

The exporter retains parallel range downloads, serial fallback, bounded retries, storage checks, MediaStore writes, foreground progress and Room persistence.
