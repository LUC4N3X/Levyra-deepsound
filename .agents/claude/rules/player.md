---
paths:
  - "app/src/main/java/com/luc4n3x/levyra/player/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/feature/player/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/feature/motion/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/viewmodel/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResilienceEngine.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/LevyraVideoStreamSelector.kt"
---

# Player and Media

- Preserve the user's explicit audio/song versus native-video choice. Never auto-switch modes merely because a decorative asset exists.
- Only the main playback pipeline may produce audible audio. Decorative motion artwork must be muted and must not own MediaSession state.
- Do not let artwork, lyrics, SponsorBlock, metadata, prefetch, or diagnostics block `startResolve` or MediaItem preparation.
- Queue, shuffle, and next-track changes must not unnecessarily restart current-track resolution or playback.
- Limit speculative work. Prefetch only what the existing policy permits, normally the current and immediate next item.
- Release players, surfaces, callbacks, and decoders on track changes, mode changes, errors, background transitions, and disposal.
- Keep PlaybackService, notification, Android Auto, widget, queue index, repeat, shuffle, and UI state consistent.
- Re-throw coroutine cancellation. Do not display cancellation as a player error.
- Test rapid track changes, same-identity replacement, mode switching, background/foreground, queue mutation, and disabled animations when touching related code.
