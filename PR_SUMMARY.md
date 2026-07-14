## Summary

- Implementazione dello stile minimale **Apple Music** all'interno dell'applicazione Levyra, mantenendo lo sfondo cinematico scuro attuale ma ottimizzando accenti e geometrie dei componenti.

## Scope

- [ ] Player / audio
- [ ] Stream extraction
- [ ] Downloads
- [x] UI / Compose
- [ ] Localization
- [ ] Android Auto / media session
- [ ] CI / release
- [ ] Documentation

## What changed

- **`LevyraTheme.kt`**: Aggiunta la costante `APPLE_MUSIC` e il relativo preset con accento cromatico blu elettrico/Apple Blue (`#0A84FF`) e grigi coordinati.
- **`LevyraPreferences.kt`** & **`MainActivity.kt`**: Impostato il tema `APPLE_MUSIC` come predefinito all'avvio dell'applicazione.
- **`LevyraApp.kt` (TabButton)**: Rimosso il contenitore a pillola animato (stile Material 3) sotto le icone attive nella barra di navigazione inferiore. Introdotto un indicatore di selezione minimalista: una linea orizzontale blu elettrico che si apre a scorrimento sotto il testo della scheda attiva ed una soffusa aura circolare blu dietro l'icona selezionata.
- **`LevyraApp.kt` (Bottoni)**: Uniformate le curvature dei pulsanti (`ArtistFollowButton`, `AlbumPrimaryPlayButton` e `AlbumSecondaryAction`) a `12.dp` (rettangolo arrotondato morbido stile Apple) anziché pillola o Material 3 standard.

## Testing

- [ ] `gradle --no-daemon :app:lintRelease`
- [ ] `gradle --no-daemon testReleaseUnitTest`
- [x] `gradle assembleDebug` (Compilazione completata con successo: `BUILD SUCCESSFUL`)
- [x] APK installato su dispositivo (Pronto sul Desktop come `Levyra-debug.apk`)
- [x] Playback tested
- [x] Downloads tested
- [x] Language switching tested

## Release safety

- [x] `levyraVersionName` and `levyraVersionCode` are correct
- [x] No secrets, keystores, APKs or ZIPs committed
- [x] No duplicated release workflows
- [x] Credits and licenses updated when adding external components

## Related issues

- Closes #
- Related to #
