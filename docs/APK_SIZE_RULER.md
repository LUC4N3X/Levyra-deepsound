# Levyra APK Size Ruler

Levyra usa Spotify Ruler per analizzare quanto pesano moduli e dipendenze dentro il bundle Android.

## Task principale

```bash
gradle --no-daemon :app:analyzeDebugBundle
```

## Report

Il workflow `APK Size Ruler` pubblica l'artifact `levyra-ruler-report-<run>` con i report generati sotto `app/build`.

## Soglie configurate

```text
downloadSizeThreshold = 90 MB
installSizeThreshold = 220 MB
abi = arm64-v8a
locale = it
screenDensity = 440
sdkVersion = 35
```

## Strategia

Il controllo gira sul bundle debug per non richiedere keystore, segreti release o pubblicazione. La release normale resta separata e continua a passare dai workflow dedicati.

## Uso consigliato

```bash
gradle --no-daemon :app:analyzeDebugBundle
```

Se il report mostra una crescita anomala, controllare prima dipendenze nuove, asset duplicati, librerie di rete, Media3, extractor e immagini non ottimizzate.
