# Home discovery redesign

This change reorganises the Android Home screen around immediate listening actions, one featured recommendation and progressively deeper discovery content.

## First viewport

- compact greeting, settings control and mood chips;
- six artwork-aware quick destinations;
- one calmer editorial spotlight;
- artwork-derived Levyra Aura atmosphere.

## Content hierarchy

Personalised listening and quick picks appear before broad exploration. Music videos move below new releases, albums and artist discovery so they no longer compete with the main listening flow.

## Behaviour preserved

The redesign reuses the existing Home ViewModel, stable render snapshot, artwork palette cache, loading policy, playback actions and interface settings. No extraction, playback, download or persistence behaviour is changed.

## Design guardrails

The root `DESIGN.md` records the shared colour, spacing, radius, component and motion rules for future UI work.
