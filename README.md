<div align="center">

# 🌊 LEVYRA

### Sound from the Deep

**Un music player Android moderno, velocissimo e gratuito — alimentato da YouTube & YouTube Music.**

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![UI](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Playback](https://img.shields.io/badge/Media3-ExoPlayer-FF4081)
![License](https://img.shields.io/badge/license-MIT-00E5FF)

</div>

---

## ✨ Cos'è LEVYRA

LEVYRA trasforma YouTube e YouTube Music in un player **pulito, sportivo e istantaneo**.
Niente account, niente pubblicità nel flusso audio, niente fronzoli: cerchi, tocchi, parte.
Stile **Apple / Vercel** con sfumature dinamiche prese dalle copertine.

## 🚀 Funzionalità

| | |
|---|---|
| ⚡ **Avvio istantaneo** | Risoluzione stream in parallelo + cache + prefetch: i brani partono appena li tocchi |
| 🏠 **Home reale** | Il vero feed di YouTube Music, in caroselli con il nostro stile |
| 📈 **Classifiche vere** | Top 50 reali per Paese (🇮🇹 🇺🇸 🇬🇧 🇪🇸 🇫🇷 🇩🇪 🇧🇷 🇲🇽 🇳🇱 🇯🇵) |
| 🔎 **Ricerca globale** | Qualsiasi brano, artista o album da YouTube Music |
| 🎛️ **Player completo** | Shuffle · Ripeti (off/tutti/uno) · Velocità 0.75×–2× · Timer di spegnimento |
| ⏯️ **Riprendi** | Riapri l'app e riparte dal punto esatto in cui eri |
| ❤️ **Preferiti** | Salvati sul dispositivo, sempre con te |
| 🔀 **Mix per te** | Riproduzione casuale dei tuoi suggeriti |
| 👋 **Onboarding** | Mini questionario gusti al primo avvio |
| ⚙️ **Impostazioni** | Animazioni, colore dinamico, e altro |
| 🖼️ **Immagini rapide** | Cache su disco + miniature ottimizzate per scrolling fluido |

## 🧱 Architettura

```text
com.luc4n3x.levyra
├── ui/            Jetpack Compose · Material 3 · tema LEVYRA
├── viewmodel/     StateFlow + UI state immutabile
├── player/        Media3 / ExoPlayer (audio focus, buffer rapido)
├── data/          YouTube Music (InnerTube + NewPipe), Apple charts, store locali
└── domain/        Modelli, mood engine, catalogo classifiche
```

- **100% Kotlin**, single-Activity, UI dichiarativa.
- Nessuna dipendenza da Google Play Services.
- Stream risolti con un *race* tra più client (Android Music, iOS, Web Remix…): vince il più veloce.

## 📦 Build dell'APK

Il modo più semplice: **GitHub Actions**.

```text
Actions → Build LEVYRA APK → artifact "levyra-apk"
```

Vengono prodotti:

```text
LEVYRA-debug.apk      (per test)
LEVYRA-release.apk    (firmato, consigliato)
```

Oppure in locale (serve Android SDK):

```bash
gradle assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

## 📲 Installazione

1. Scarica `LEVYRA-release.apk`.
2. Aprilo: Android chiederà di consentire l'installazione da questa origine — è normale per le app fuori dal Play Store.
3. Conferma e goditi la musica. 🎧

> L'APK release è firmato con una chiave stabile, quindi gli aggiornamenti si installano sopra senza disinstallare.

## 🎚️ Comportamento di riproduzione

LEVYRA risolve **solo** il brano selezionato: non sostituisce mai una traccia con un'alternativa.
Se YouTube blocca un video in modo anonimo, la riproduzione si ferma e mostra l'errore esatto invece di partire con un altro brano.

## 🛠️ Tech stack

`Kotlin` · `Jetpack Compose` · `Material 3` · `Media3 / ExoPlayer` · `Coil` · `OkHttp` · `NewPipeExtractor` · `Coroutines`

## 📄 Licenza

MIT © LUC4N3X

<div align="center">

**LEVYRA** — _Sound from the Deep_ 🐋

</div>
