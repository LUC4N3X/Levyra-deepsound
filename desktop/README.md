# Levyra Desktop

Client desktop di Levyra per Windows, scritto in Kotlin con Compose Multiplatform.
Condivide l'estrattore YouTube del progetto Android (`third_party/LevyraExtractor`), riusa lo stesso catalogo di localizzazione e riproduce l'audio con libvlc.

## Funzioni

- Home musicale come schermata iniziale
- Prima configurazione guidata con lingua, nome, gusti musicali e Paese dei contenuti
- Libreria completa con playlist, preferiti, download offline e cronologia
- Download audio persistenti con avanzamento, annullamento, ripresa, nuovo tentativo e cancellazione
- Riproduzione automatica del file locale quando un brano è già disponibile offline
- Ricerca YouTube Music per brani, video, album, playlist e artisti, con suggerimenti e paginazione
- Suggerimenti rapidi, artisti e atmosfere localizzati in base alla lingua e al Paese
- Classifiche Top 50 selezionabili tramite bandiera e nome del Paese
- Testi sincronizzati da LRCLIB, con la riga corrente evidenziata durante l'ascolto
- Coda con shuffle e ripetizione, radio automatica a fine coda
- Player desktop completo, chiudibile e dotato di comando per il download offline
- Mini player separato, ridimensionabile e sempre in primo piano
- Protezione single-instance: una seconda apertura riporta in primo piano la finestra già attiva
- Protocollo `levyra://` e apertura diretta di link YouTube e YouTube Music
- Controllo automatico degli aggiornamenti Windows con verifica SHA-256
- Rapporto di arresto imprevisto salvato localmente e copiabile dalla finestra di errore
- Colore d'accento estratto dalla copertina, tema chiaro e scuro e barra titolo Windows coordinata
- Icona ufficiale Levyra in finestra, tray, sidebar e installer

## Prima apertura

Quando non esiste ancora un profilo completato, Levyra mostra la configurazione iniziale prima della Home:

1. scelta della lingua;
2. nome visualizzato nell'app;
3. scelta di almeno tre gusti musicali;
4. scelta del Paese usato per contenuti e Top 50.

Il profilo viene salvato in locale. Nome, lingua e Paese possono essere modificati in Impostazioni e il questionario può essere riaperto senza cancellare libreria, playlist, download o cronologia.

## Libreria

La voce Libreria nella barra laterale raccoglie le quattro aree personali dell'app:

- playlist locali;
- brani preferiti;
- download offline;
- cronologia degli ascolti.

Ogni sezione mostra il proprio conteggio e permette di riprodurre, aggiungere alla coda, aggiungere a una playlist o gestire il contenuto senza tornare alla Home.

## Download offline

Il comando di download è disponibile nel menu di ogni riga brano e direttamente nel player. I download vengono salvati in `%APPDATA%\Levyra\offline` e registrati in `downloads.json`.

Il motore di download:

- esegue al massimo due trasferimenti contemporaneamente;
- salva l'avanzamento su disco;
- usa file temporanei `.part`;
- riprende i trasferimenti interrotti tramite richieste HTTP Range;
- finalizza il file con spostamento atomico;
- permette annullamento, nuovo tentativo e cancellazione;
- verifica all'avvio che i file completati esistano ancora;
- preferisce automaticamente il file locale durante la riproduzione.

I download incompleti restano disponibili per la ripresa dopo la chiusura o un riavvio dell'app. I file completati vengono riprodotti senza richiedere una nuova risoluzione dello stream.

## Mini player e lifecycle desktop

Il mini player è una finestra separata sempre in primo piano. Condivide in tempo reale lo stato del player principale, ricorda posizione e dimensione e supporta:

- play e pausa;
- brano precedente e successivo;
- preferiti;
- avanzamento del brano;
- apertura della finestra principale;
- scorciatoie Spazio, Freccia sinistra, Freccia destra ed Esc.

Levyra mantiene una sola istanza attiva. Una seconda apertura non inizializza nuovamente libreria, download o player: invia una richiesta locale all'istanza già attiva e la riporta in primo piano.

## Link diretti

Su Windows viene registrato il protocollo utente `levyra://` senza richiedere privilegi amministrativi.

Esempi:

```text
levyra://open?url=https%3A%2F%2Fmusic.youtube.com%2Fplaylist%3Flist%3D...
levyra://search?q=artista
levyra://watch?v=VIDEO_ID
```

Sono accettati anche URL diretti di YouTube, YouTube Music e `youtu.be`. Se Levyra è già aperta, il link viene inoltrato all'istanza esistente.

## Aggiornamenti Windows

Android e Desktop hanno versionamento completamente separato.

La versione Windows si modifica soltanto nel file:

```properties
# desktop/version.properties
levyraDesktopVersion=2.3.16
```

Il valore Android `levyraVersionName` nel `gradle.properties` principale non controlla, non avvia e non pubblica la build Desktop.

Le release Windows usano tag indipendenti:

```text
desktop-v2.3.16
desktop-v2.3.17
```

L'app controlla esclusivamente le release con prefisso `desktop-v`. Quando trova una versione superiore:

1. mostra la notifica tradotta;
2. scarica l'MSI Windows corretto;
3. verifica il file `.sha256` pubblicato insieme all'installer;
4. chiude l'app;
5. installa la nuova versione sopra quella esistente;
6. riapre Levyra.

Il valore stabile `upgradeUuid` identifica tutte le versioni Windows come la stessa applicazione installata.

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

La build è indipendente da quella Android: `desktop/` ha il proprio `settings.gradle.kts`, il proprio catalogo delle dipendenze, il proprio wrapper e il proprio file di versione.

## Struttura

```text
desktop/
  version.properties
  core/      modelli, estrattore YouTube, risoluzione stream, download e persistenza
  player/    astrazione del player audio e implementazione libvlc, coda di riproduzione
  app/       interfaccia Compose, onboarding, libreria, lifecycle, aggiornamenti e packaging Windows
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
./gradlew createReleaseDistributable
./gradlew packageReleaseMsi
./gradlew packageReleaseExe
```

Gli artefatti finiscono in `app/build/compose/binaries/main-release/`. Il workflow legge automaticamente `levyraDesktopVersion` da `desktop/version.properties`.

Dopo il merge su `main`, MSI, EXE, ZIP portabile e checksum SHA-256 vengono pubblicati nella release indipendente `desktop-v<versione>`. Le release Android `v<versione>` restano separate e non vengono modificate dal workflow Desktop. La PR genera soltanto artefatti temporanei.

## Runtime VLC

All'avvio della riproduzione Levyra cerca libvlc in questo ordine:

1. cartella indicata in Impostazioni (`vlcDirectory`);
2. cartella `vlc` distribuita con l'applicazione;
3. variabili d'ambiente `LEVYRA_VLC_PATH` e `VLC_HOME`;
4. installazioni standard in `Program Files\VideoLAN\VLC`.

Per distribuire l'app senza chiedere l'installazione di VLC, copiare `libvlc.dll`, `libvlccore.dll` e la cartella `plugins` di una installazione VLC a 64 bit dentro `desktop/app/resources/windows-x64/vlc/` prima del packaging.

## Dati locali

Preferenze, libreria, download e cache delle copertine sono salvati in `%APPDATA%\Levyra` su Windows:

| Percorso | Contenuto |
|---|---|
| `settings.json` | profilo, onboarding, gusti, audio, tema, lingua, Paese e percorso VLC |
| `library.json` | preferiti, playlist locali, cronologia, ricerche recenti |
| `downloads.json` | coda, stato, avanzamento e metadati dei download offline |
| `offline/` | file audio completati e file temporanei ripristinabili |
| `updates/` | installer temporanei, checksum e log degli aggiornamenti |
| `crash-reports/` | rapporti locali degli arresti imprevisti |
| `session.json` | coda e posizione dell'ultima sessione |
| `window.json` | dimensione e posizione della finestra principale |
| `cache/artwork` | cache su disco delle copertine |

## Scorciatoie

| Tasto | Azione |
|---|---|
| `Spazio` | play o pausa |
| `Ctrl` + `→` | brano successivo nella finestra principale |
| `Ctrl` + `←` | brano precedente nella finestra principale |
| `→` | brano successivo nel mini player |
| `←` | brano precedente nel mini player |
| `Esc` | chiude il mini player |

## Riferimenti tecnici

Il rafforzamento del lifecycle Desktop ha preso come riferimento architetturale il progetto GPL-3.0 SimpMusic, in particolare i pattern relativi a single-instance, mini player, gestione dei crash, deep link e packaging desktop. L'implementazione Levyra è stata riscritta e adattata al proprio stato, al proprio player libvlc e al proprio sistema di persistenza.

## Vincoli di design

L'interfaccia mantiene l'icona ufficiale Levyra, l'onboarding localizzato, i menu Paese con bandiera e nome nativo, il layout RTL per arabo ed ebraico, la libreria integrata e il player chiudibile. Non vengono introdotti campi manuali per i codici Paese né cataloghi di traduzione desktop separati che possano divergere dall'APK.
