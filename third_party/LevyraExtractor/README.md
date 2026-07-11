<div align="center">

# LevyraExtractor

**The extraction engine built for Levyra.**

[![Release](https://img.shields.io/github/v/release/LUC4N3X/LevyraExtractor?style=flat-square&logo=github)](https://github.com/LUC4N3X/LevyraExtractor/releases)
[![JitPack](https://img.shields.io/jitpack/version/com.github.LUC4N3X/LevyraExtractor?style=flat-square)](https://jitpack.io/#LUC4N3X/LevyraExtractor)
[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square)](LICENSE)

Reliable media extraction, stream resolution and playback support for Levyra.

</div>

---

## About

LevyraExtractor is the media extraction layer used by **Levyra**.

It reads metadata, discovers playable audio and video streams, selects suitable formats and gives the app clearer information when a source cannot be resolved.

The project is based on the NewPipe and PipePipe ecosystem, with additional work focused on Levyra's playback needs.

This is an independent downstream project. It is not an official NewPipe, PipePipe, Metrolist, Google or YouTube repository.

## Main features

- YouTube audio and video stream resolution
- Android VR, Safari and TVHTML5 multi-client stream resolution
- SABR stream discovery
- Cross-client direct stream aggregation with one batched URL deobfuscation pass
- Automatic format selection
- Fast short-lived caching
- Duplicate request sharing
- Playback and fallback diagnostics
- SponsorBlock integration
- Return YouTube Dislike integration
- Support for YouTube, SoundCloud, Bandcamp, PeerTube, media.ccc.de, BiliBili and NicoNico

## Why it exists

Levyra needs an extractor that is fast, predictable and easy to diagnose when upstream services change.

LevyraExtractor keeps the proven foundation of its upstream projects while adding a dedicated resolution layer for the Levyra player.

The main goal is simple: make playback more reliable without hiding failures or making the host application harder to maintain.

The current playback path resolves Android VR, Safari and TVHTML5 independently, preserves the CPN generated for each client, merges direct audio, muxed video and video-only formats, and falls back to SABR or HLS only when direct streams are unavailable. A failed client no longer invalidates a valid response returned by another client.

## Requirements

LevyraExtractor targets **Java 17** and is distributed through **JitPack**.

The host application must provide its own downloader and remains responsible for network timeouts, retries, cancellation and response cleanup.

Release information and published versions are available from the repository releases and JitPack pages.

## Credits

LevyraExtractor would not exist without the work of the open-source community.

Core upstream projects:

- [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) — original extractor architecture and service implementations
- [NewPipe](https://github.com/TeamNewPipe/NewPipe) — original application and ecosystem
- [PipePipe](https://codeberg.org/NullPointerException/PipePipe) — extended downstream ecosystem
- [PipePipeExtractor](https://github.com/InfinityLoop1308/PipePipeExtractor) — main downstream extractor lineage
- [MetrolistExtractor](https://github.com/MetrolistGroup/MetrolistExtractor) — reference work for modern YouTube playback handling
- [Metrolist](https://github.com/MetrolistGroup/Metrolist) — open-source YouTube Music client ecosystem
- [SponsorBlock](https://github.com/ajayyy/SponsorBlock) — community skip-segment data
- [Return YouTube Dislike](https://github.com/Anarios/return-youtube-dislike) — community rating data

Special thanks to **Christian Schabesberger**, **Team NewPipe**, and every NewPipe, PipePipe, Metrolist and downstream contributor whose work remains part of this codebase.

The project also uses open-source libraries including jsoup, OkHttp, Wire, Protocol Buffers, nanojson, Java-WebSocket, Brotli, Apache Commons and SpotBugs Annotations.

All original copyright notices and licenses remain available in the source tree. Names and trademarks belong to their respective owners.

## Disclaimer

Online services can change without notice, so extraction may occasionally require updates.

Use this project responsibly and respect copyright, service terms and applicable laws.

## License

LevyraExtractor is distributed under the **GNU General Public License v3.0**.

See [LICENSE](LICENSE) for the complete license text.

---

<div align="center">

Built for **Levyra** by [LUC4N3X](https://github.com/LUC4N3X)

</div>
