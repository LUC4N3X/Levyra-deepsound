# Levyra Player Mobius Sample Architecture

Questa struttura introduce il modello `Model / Event / Effect / Update / Init / ViewData` ispirato al sample Android di Mobius, senza cambiare il player attuale e senza aggiungere dipendenze runtime.

## Struttura

```text
app/src/main/java/com/luc4n3x/levyra/architecture/mobius
└── LevyraLoopPrimitives.kt

app/src/main/java/com/luc4n3x/levyra/feature/player
├── bridge
│   └── LevyraPlayerStateBridge.kt
├── domain
│   ├── PlayerEffect.kt
│   ├── PlayerEvent.kt
│   ├── PlayerInit.kt
│   ├── PlayerModel.kt
│   └── PlayerUpdate.kt
└── presentation
    └── PlayerViewData.kt
```

## Flusso

```text
PlayerEvent
→ PlayerUpdate
→ PlayerModel aggiornato
→ PlayerEffect da eseguire fuori dal reducer
→ PlayerViewData per la UI
```

## Cosa resta puro

```text
PlayerModel
PlayerEvent
PlayerEffect
PlayerInit
PlayerUpdate
PlayerViewDataMapper
```

Questi file non devono conoscere Android, Media3, repository, coroutine scope, resolver reali o Compose.

## Cosa resta impuro

```text
PlaybackResolver
LevyraPlayer
LyricsRepository
LevyraPreferences
LevyraWidgetBridge
Media3 PlaybackService
```

Questi componenti vanno collegati tramite effect handler quando il refactor del player verrà completato.

## Migrazione sicura

1. Usare `LevyraPlayerStateBridge.fromUiState(state)` per osservare l'equivalente `PlayerModel` senza cambiare comportamento.
2. Portare prima i test delle transizioni `play / pause / resolve / seek / error` su `PlayerUpdate`.
3. Collegare gradualmente gli effetti a resolver, player e lyrics.
4. Solo dopo sostituire la logica player dentro `LevyraViewModel`.

## Regola di architettura

La UI non deve decidere retry, resolve, snapshot, lyrics o skip. La UI emette eventi. Il reducer decide stato ed effetti. Gli effect handler eseguono lavoro esterno e rimandano eventi di risultato.
