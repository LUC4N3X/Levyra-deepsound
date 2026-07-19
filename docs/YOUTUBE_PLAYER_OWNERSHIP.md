# Owning the YouTube Playback Stack

**Purpose:** make Levyra self-sufficient on the parts of YouTube playback that break
periodically, so that when upstream YouTube changes something, you can diagnose and
fix it yourself instead of waiting on any external project.

This document is a runbook, not a tutorial. It answers one question:

> Playback just broke. Which layer failed, which file do I touch, and how do I verify the fix?

---

## 0. First principle: nothing is copied *from* YouTube

The YouTube player is closed, proprietary software. There is no "YouTube player source"
to import, and Levyra does not contain any. What makes Levyra a YouTube player is the
ability to speak YouTube's private **InnerTube** protocol.

Two categories of thing are involved, and only one of them is code:

| | What it is | Where it comes from |
|---|---|---|
| **Code** | The logic that speaks InnerTube (clients, JS decode, format selection, SABR) | Levyra's own tree (`LevyraExtractor` + app), GPL-3.0 |
| **Runtime data** | `base.js`, current client versions, BotGuard challenge, PO Tokens | Fetched live from YouTube at runtime |

We already own all the code. "Making the player ours" therefore means **owning the
maintenance** of the handful of fragile spots below — not acquiring anything new.

---

## 1. Stack map

### Extractor layer — Java
`third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/`

| File | Responsibility |
|---|---|
| `YoutubeStreamExtractor.java` | Player response → available formats |
| `YoutubeJavaScriptExtractor.java` | Fetches the player `base.js` from YouTube |
| `YoutubeJavaScriptDecoder.java` | Signature decipher **and** `n`-parameter (throttling) transform |
| `YoutubeJavaScriptPlayerManager.java` | Orchestrates/caches the player JS and the decoders |
| `ClientsConstants.java`, `InnertubeClientRequestInfo.java` | Client identities (ANDROID_VR, Web, iOS…) and their versions |
| `ItagItem.java`, `DeliveryType.java` | Format/itag metadata and selection |
| `sabr/` (≈50 files) | SABR/UMP adaptive streaming protocol |
| `sabr/SabrPoTokenProvider.java`, `sabr/SabrColdStartPoToken.java` | PO Token plumbing inside the extractor |
| `levyra/LevyraYoutubeResolver.java` | Levyra's multi-client resolution entry point |

### App layer — Kotlin
`app/src/main/java/com/luc4n3x/levyra/`

| File | Responsibility |
|---|---|
| `data/PlaybackResolver.kt` | The extractor ⇄ InnerTube race, in-flight dedup, cached-URL validation |
| `data/YoutubePlaybackSecurity.kt` | Isolated WebView BotGuard runtime + PO Token generation, guest session |
| `data/YoutubeLocalDecoder.kt` | On-device decode support |
| `data/security/GoogleApiKeyHeaders.kt` | InnerTube request headers |
| `data/PlaybackResilienceEngine.kt` | Recovery, client rotation, session rotation |
| `data/network/LevyraHttpClientFactory.kt` | Shared bounded-timeout HTTP client |
| `player/PlaybackService.kt`, `player/LevyraPlayer.kt` | Media3 / ExoPlayer + MediaSession |

---

## 2. The three fragile layers (the only ones that break often)

Everything else in section 1 changes rarely. In practice, ~95% of "YouTube playback
broke" incidents are one of these three, in this order of frequency:

### 2.1 Signature cipher
- **What it is:** YouTube ships stream URLs whose access is gated by a value derived from
  `signatureCipher` / `s`, transformed by a function inside `base.js`.
- **Owned by:** `YoutubeJavaScriptDecoder.java` (decipher path), driven by
  `YoutubeJavaScriptPlayerManager.java`.
- **Symptom:** formats parse fine, but every stream URL returns **HTTP 403** and nothing
  plays at all.

### 2.2 `n`-parameter (throttling)
- **What it is:** the `n` query parameter must be transformed by another `base.js`
  function; a wrong/absent transform makes YouTube throttle the stream.
- **Owned by:** `YoutubeJavaScriptDecoder.java` (the `n` transform path).
- **Symptom:** playback *starts* but constantly **buffers/stalls** (throttled to a few
  tens of kB/s), or intermittently 403s. Downloads crawl.

### 2.3 PO Token / BotGuard
- **What it is:** an attestation proving the request isn't an anonymous bot, produced by a
  BotGuard challenge run in an isolated WebView.
- **Owned by:** `data/YoutubePlaybackSecurity.kt` (app) + `sabr/SabrPoTokenProvider.java`,
  `sabr/SabrColdStartPoToken.java` (extractor).
- **Symptom:** `LOGIN_REQUIRED`, "Sign in to confirm you're not a bot", missing formats,
  or 403s that come *with a bot-detection message*. Guest-session rotation starts firing.

---

## 3. Diagnosis decision tree

```text
Playback broke
├── Formats missing / "sign in to confirm" / LOGIN_REQUIRED
│      → PO Token / BotGuard  (§2.3 → YoutubePlaybackSecurity.kt, SabrPoToken*)
│
├── Formats present, URL 403, NOTHING plays
│      → Signature cipher     (§2.1 → YoutubeJavaScriptDecoder decipher path)
│
├── Plays but constantly buffers / throttled / very slow download
│      → n-parameter          (§2.2 → YoutubeJavaScriptDecoder n path)
│
├── Only one client fails, another still works
│      → client identity stale (ClientsConstants.java version bump), not a JS break
│
└── Geo-restricted / age-gated only
       → NOT a break. Do not rotate the session (see extractor-network rule).
```

> The distinction in the last two branches matters: a **stale client version** is a
> one-line bump in `ClientsConstants.java`; a **geo restriction** is expected behaviour and
> must not trigger security-session churn. Both are routinely mistaken for a decoder break.

---

## 4. Upstream sync playbook (how to fix without a rewrite)

We stay independent by **watching**, not by depending. When §2.1/§2.2 break, the fix is
almost always already published upstream within days.

**Watch these two sources for signature/`n` fixes:**
- `TeamNewPipe/NewPipeExtractor` — same Java lineage as us; diffs apply almost directly.
- `yt-dlp/yt-dlp` — usually the first to publish a new `nsig`/signature approach.

**Cherry-pick procedure (safe):**
1. Reproduce the break locally and confirm the *layer* with §3.
2. Find the corresponding upstream change (it will touch the JS-decoder equivalent).
3. Port **only** the decoder/transform logic into `YoutubeJavaScriptDecoder.java` /
   `…PlayerManager.java`. **Do not** import their resolver, client list, or SABR — those
   diverged on purpose and carry Levyra-specific work.
4. Keep our multi-client race (`LevyraYoutubeResolver` / `PlaybackResolver.kt`) untouched:
   a decoder fix must not reserialize the fast path or demote ANDROID_VR.
5. Preserve every upstream copyright/notice; update `THIRD_PARTY_NOTICES.md` if new code
   lands.

**Invariants a fix must not violate** (from `.claude/rules/`):
- No hardcoded keys, cookies, PO Tokens, or visitor data.
- Keep the last-known-good decoder path; don't invalidate a working config before the
  replacement is validated.
- Distinguish a conclusive no-match from timeout/transport/HTTP/parse failures.
- Every resolver/decoder change ships with a focused regression test.

---

## 5. Verifying a fix

```bash
# Extractor-level resolver regression (fastest signal).
# LevyraExtractor is a composite build (see settings.gradle.kts: includeBuild),
# so target its module through its own project directory:
./gradlew --no-daemon -p third_party/LevyraExtractor :extractor:test --tests "*LevyraYoutubeResolver*"

# App release gates
./gradlew --no-daemon :app:lintRelease
./gradlew --no-daemon testReleaseUnitTest
./gradlew --no-daemon assembleRelease
```

Then, on a device, confirm all three symptoms are gone:
1. a fresh track **plays** (rules out signature),
2. it plays **without buffering** and a download runs at full speed (rules out `n`),
3. a cold start with no cached session still resolves (rules out PO Token / BotGuard).

---

## 6. Golden rule

> We do not chase parity with any upstream, and we do not copy YouTube.
> We own three fragile functions — signature, `n`, PO Token — and we keep a
> known-good path behind each. Everything else is already ours and stays put.
