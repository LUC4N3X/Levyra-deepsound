---
name: Levyra Motion Artwork
description: Implements or reviews decorative Apple/Tidal motion artwork without breaking Levyra's existing audio/video choice, playback, security, or resource limits.
paths:
  - "app/src/main/java/com/luc4n3x/levyra/feature/motion/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/ui/MotionArtworkLayer.kt"
  - "app/src/main/java/com/luc4n3x/levyra/viewmodel/**/*.kt"
---

# Motion artwork workflow

- Motion artwork belongs only to song/audio mode. Never replace or overlay the native music-video mode.
- Show static artwork immediately and keep it as the permanent fallback.
- Keep the decorative player muted, bounded, lifecycle-aware, and independent from audible MediaSession playback.
- Require canonical track/album and primary-artist compatibility. Preserve exact grouped artist signatures such as `Earth, Wind & Fire`.
- Validate HTTPS, approved provider media hosts, port, resolved public addresses, MIME, and every redirect hop.
- Deduplicate lookups in an engine-owned supervised scope; caller cancellation must not poison other waiters or the in-flight map.
- Use generation plus identity checks before publishing or clearing ViewModel ownership.
- Negative-cache only conclusive no-match/invalid results, never transient provider or verifier failures.
- Limit prefetch to the immediate next track and do not instantiate its decoder.
- Test primary-versus-guest artist, grouped names, redirect/host rejection, transient failures, cancellation, stale publication, and static fallback.
