# Levyra Desktop

Client desktop di Levyra per Windows, scritto in Kotlin con Compose Multiplatform.
Condivide l'estrattore YouTube del progetto Android (`third_party/LevyraExtractor`)
tramite composite build, riusa lo stesso catalogo di localizzazione e riproduce l'audio con libvlc.

## Funzioni

- Home musicale come schermata iniziale
- Prima configurazione guidata con lingua, nome, gusti musicali e Paese dei contenuti
- Libreria locale con preferiti, playlist, cronologia e ripresa della sessione
- Ricerca YouTube Music per brani, video, album, playlist e artisti, con suggerimenti e paginazione
- Suggerimenti rapidi, artisti e atmosfere localizzati in base alla lingua e al Paese
- Classifiche Top 50 selezionabili tramite bandiera e nome del Paese
- Testi sincronizzati da LRCLIB, con la riga corrente evidenziata durante l'ascolto
- Coda con shuffle e ripetizione, radio automatica a fine coda
- Player desktop completo e chiudibile
- Colore d'accento estratto dalla copertina, tema chiaro e scuro e barra titolo Windows coordinata
- Icona ufficiale Levyra in finestra, tray, sidebar e installer

## Prima apertura

Quando non esiste ancora un profilo completato, Levyra mostra la configurazione iniziale prima della Home:

1. scelta della lingua;
2. nome visualizzato nell'app;
3. scelta di almeno tre gusti musicali;
4. scelta del Paese usato per contenuti e Top 50.

Il profilo viene salvato in locale. Nome, lingua e Paese possono essere modificati in Impostazioni e il questionario può essere riaperto senza cancellare libreria, playlist o cronologia.

## Lingue

La versione desktop compila direttamente lo stesso catalogo di traduzioni dell'APK Android. Sono supportate 26 lingue:

- English
- Italiano
- Español
- Français
- Deutsch
- Português
- Nederlands
- Polski
- Română
- Ελληνικά
- Svenska
- Dansk
- Čeština
- Українська
- Русский
- Türkçe
- العربية
- 简体中文
- 日本語
- 한국어
- हिन्दी
- Bahasa Indonesia
- Tiếng Việt
- ไทย
- Filipino
- עברית

Arabo ed ebraico attivano il layout RTL nell'onboarding e nell'intera interfaccia desktop.

## Requisiti

| Componente | Versione |
|---|---|
| JDK | 21 (Temurin consigliato) |
| Gradle | wrapper incluso (9.6.1) |
| VLC | 3.0.x a 64 bit, oppure runtime libvlc distribuito con l'app |
| WiX Toolset | 3.14 (solo per generare `.msi` e `.exe`) |

La build è indipendente da quella Android: `desktop/` ha il proprio
`settings.gradle.kts`, il proprio catalogo delle dipendenze e il proprio wrapper.

## Struttura

```
desktop/
  core/      modelli, estrattore YouTube, risoluzione stream, persistenza
  player/    astrazione del player audio e implementazione libvlc, coda di riproduzione
  app/       interfaccia Compose, onboarding, stato applicativo, packaging Windows
  packaging/ icona Windows usata da jpackage
```

- `core` non dipende da Compose: è puro Kotlin/JVM e contiene la logica testabile.
- `player` espone `AudioPlayer` e `PlayerQueue`; `VlcAudioPlayer` è l'unica classe che tocca le API native.
- `app` collega tutto in `AppContainer`, riusa il catalogo i18n Android e contiene l'interfaccia desktop.

## Sviluppo

```bash
cd desktop
./gradlew run
./gradlew check
./gradlew assemble check
```

Su Windows usare `gradlew.bat` al posto di `./gradlew`.

## Pacchetti Windows

```bash
cd desktop
./gradlew createReleaseDistributable -PlevyraDesktopVersion=2.3.16
./gradlew packageReleaseMsi -PlevyraDesktopVersion=2.3.16
./gradlew packageReleaseExe -PlevyraDesktopVersion=2.3.16
```

Gli artefatti finiscono in `app/build/compose/binaries/main-release/`.
Il workflow legge automaticamente `levyraVersionName` dal `gradle.properties` principale.

Dopo il merge su `main`, MSI, EXE, ZIP portabile e checksum SHA-256 vengono aggiunti alla release GitHub già esistente della stessa versione, accanto all'APK Android. La PR genera soltanto artefatti temporanei.

## Runtime VLC

All'avvio della riproduzione Levyra cerca libvlc in questo ordine:

1. cartella indicata in Impostazioni (`vlcDirectory`);
2. cartella `vlc` distribuita con l'applicazione;
3. variabili d'ambiente `LEVYRA_VLC_PATH` e `VLC_HOME`;
4. installazioni standard in `Program Files\VideoLAN\VLC`.

Per distribuire l'app senza chiedere l'installazione di VLC, copiare
`libvlc.dll`, `libvlccore.dll` e la cartella `plugins` di una installazione VLC
a 64 bit dentro `desktop/app/resources/windows-x64/vlc/` prima del packaging.

## Dati locali

Preferenze, libreria e cache delle copertine sono salvate in
`%APPDATA%\Levyra` su Windows:

| File | Contenuto |
|---|---|
| `settings.json` | profilo, onboarding, gusti, audio, tema, lingua, Paese e percorso VLC |
| `library.json` | preferiti, playlist locali, cronologia, ricerche recenti |
| `session.json` | coda e posizione dell'ultima sessione |
| `window.json` | dimensione e posizione della finestra |
| `cache/artwork` | cache su disco delle copertine |

## Scorciatoie

| Tasto | Azione |
|---|---|
| `Spazio` | play o pausa |
| `Ctrl` + `→` | brano successivo |
| `Ctrl` + `←` | brano precedente o riavvio |

## Vincoli di design

L'interfaccia mantiene l'icona ufficiale Levyra, l'onboarding localizzato, i menu Paese con bandiera e nome nativo, il layout RTL per arabo ed ebraico e il player chiudibile. Non vengono introdotti campi manuali per i codici Paese né cataloghi di traduzione desktop separati che possano divergere dall'APK.
