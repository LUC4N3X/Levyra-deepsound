# Levyra Desktop

Client desktop di Levyra per Windows, scritto in Kotlin con Compose Multiplatform.
Condivide l'estrattore YouTube del progetto Android (`third_party/LevyraExtractor`)
tramite composite build e riproduce l'audio con libvlc.

## Funzioni

- Libreria locale con preferiti, playlist, cronologia e ripresa della sessione
- Ricerca YouTube Music per brani, video, album, playlist e artisti, con suggerimenti e paginazione
- Classifiche dei brani più ascoltati per paese, con risoluzione automatica su YouTube alla riproduzione
- Testi sincronizzati da LRCLIB, con la riga corrente evidenziata durante l'ascolto
- Coda con shuffle e ripetizione, radio automatica a fine coda
- Interfaccia con colore d'accento estratto dalla copertina in riproduzione, tema chiaro e scuro, italiano e inglese

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
  app/       interfaccia Compose, stato applicativo, packaging Windows
  packaging/ icona Windows usata da jpackage
```

- `core` non dipende da Compose: è puro Kotlin/JVM e contiene la logica testabile.
- `player` espone `AudioPlayer` e `PlayerQueue`; `VlcAudioPlayer` è l'unica classe
  che tocca le API native.
- `app` collega tutto in `AppContainer` e non contiene logica di dominio.

## Sviluppo

```bash
cd desktop
./gradlew run              # avvia l'app
./gradlew check            # test di core e player
./gradlew assemble check   # compilazione completa piu test
```

Su Windows usare `gradlew.bat` al posto di `./gradlew`.

## Pacchetti Windows

```bash
cd desktop
./gradlew createReleaseDistributable   # cartella eseguibile, nessun installer richiesto
./gradlew packageReleaseMsi            # installer .msi
./gradlew packageReleaseExe            # installer .exe
```

Gli artefatti finiscono in `app/build/compose/binaries/main-release/`.
La versione del pacchetto si imposta con `-PlevyraDesktopVersion=1.2.0`.

La stessa sequenza gira in CI nel workflow `Desktop Windows Build`, che pubblica
installer e build portabile come artefatti.

## Runtime VLC

All'avvio della riproduzione Levyra cerca libvlc in questo ordine:

1. cartella indicata in Impostazioni (`vlcDirectory`);
2. cartella `vlc` distribuita con l'applicazione;
3. variabili d'ambiente `LEVYRA_VLC_PATH` e `VLC_HOME`;
4. installazioni standard in `Program Files\VideoLAN\VLC`.

Per distribuire l'app senza chiedere l'installazione di VLC, copiare
`libvlc.dll`, `libvlccore.dll` e la cartella `plugins` di una installazione VLC
a 64 bit dentro `desktop/app/resources/windows-x64/vlc/` prima del packaging:
il contenuto viene incluso nell'installer e rilevato automaticamente.

## Dati locali

Preferenze, libreria e cache delle copertine sono salvate in
`%APPDATA%\Levyra` (su Linux `~/.local/share/Levyra`, su macOS
`~/Library/Application Support/Levyra`):

| File | Contenuto |
|---|---|
| `settings.json` | preferenze audio, tema, lingua, percorso VLC |
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
