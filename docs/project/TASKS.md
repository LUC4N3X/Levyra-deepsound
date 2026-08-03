# Levyra Active Tasks

## Active phase

**Name:** Android product experience shell  
**Roadmap track:** Android interface and discovery  
**Status:** Implementation complete on branch; pull-request validation pending  
**Scope:** Improve Android navigation, Home entry points, search presentation,
settings discovery, and the persistent mini player without changing playback,
network, persistence, download, version, signing, or release behavior.

## Acceptance criteria

- Primary navigation keeps Home, Search, Explore, and Library immediately visible.
- The full player remains contextual and opens from the persistent mini player.
- Home exposes clear shortcuts and useful library/listening counts without
  replacing the existing editorial feed.
- Search presents filters and grouped songs, artists, albums, recent searches,
  loading, empty, and error states through the existing ViewModel state.
- Settings exposes a structured overview with safe direct toggles and a route to
  the complete existing settings screen.
- The product shell is hidden for PiP, the full player, onboarding, details,
  queue, lyrics, update prompts, shared media, playlists, and other blocking
  overlays.
- Existing player, queue, MediaSession, notification, Android Auto, downloads,
  Room, preferences, backups, localization, and release versions remain intact.

## Work items

- [x] Add pure product-shell navigation and clearance policy.
- [x] Add a dedicated Compose product shell over the existing architecture.
- [x] Add a cleaner primary navigation surface.
- [x] Add a persistent product-style mini player.
- [x] Add Home quick actions and listening/library counters.
- [x] Add a grouped, filterable search presentation.
- [x] Add a structured settings overview with safe direct controls.
- [x] Hide the shell when an existing blocking screen owns the interaction.
- [x] Add focused unit coverage for navigation visibility and layout clearance.
- [ ] Run Android unit tests in CI.
- [ ] Run Android lint and release compilation in CI.
- [ ] Review automated findings and resolve actionable issues.
- [ ] Perform manual device checks for narrow screens, RTL, large fonts,
      navigation, search, settings, mini-player controls, and PiP transitions.

## Validation matrix

| Check | Required | Current state |
| --- | --- | --- |
| Product shell policy unit tests | Yes | Added; execution pending CI |
| Android unit tests | Yes | Pending pull-request CI |
| Android lint | Yes | Pending pull-request CI |
| Android release compile | Yes | Pending pull-request CI |
| Agent configuration validation | No | No agent configuration changed |
| Desktop build | No | Desktop files unchanged |
| Manual narrow-screen and large-font review | Yes | Pending device validation |
| Manual RTL review | Yes | Pending device validation |
| Playback, notification, Android Auto, PiP | Yes | Existing ownership preserved; manual regression check pending |
| Code review automation | Yes | Pending pull-request review |

## Behavior preserved

- Playback continues through the existing ViewModel, player, service, MediaSession,
  queue, notification, and Android Auto architecture.
- Search continues through the existing repository, jobs, immutable state, and
  result models.
- Settings changes continue through the existing persistence methods.
- Downloads, favorites, playlists, history, followed artists, queue, lyrics,
  onboarding, backups, and user preferences are not migrated or reset.
- Android and Desktop versions, dependencies, signing, packaging, tags, and
  releases are unchanged.

## Update rule

Update this phase only from direct code changes, CI results, review findings,
manual checks, or owner decisions. Do not mark blocked or unexecuted validation as
passed.
