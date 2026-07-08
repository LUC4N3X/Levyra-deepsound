# APK Size Report

Levyra usa un workflow GitHub Actions solo per generare un report della dimensione di APK e AAB debug.

Non vengono applicati limiti hard di download, install size, APK size o AAB size.

Il task diretto `:app:analyzeDebugBundle` di Spotify Ruler Ã¨ stato disattivato perchÃ© nella combinazione AGP/Gradle attuale fallisce internamente con un cast `ApplicationExtensionImpl` -> `BaseExtension`.

Il workflow ora esegue:

```text
:app:bundleDebug
:app:assembleDebug
```

Poi genera:

```text
build/size-report/apk-size-report.md
build/size-report/apk-size-report.json
```

Il report serve solo per visibilitÃ  tecnica. Non blocca la PR in base alla dimensione dei file generati.