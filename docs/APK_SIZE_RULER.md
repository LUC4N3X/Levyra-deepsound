# APK Size Guard

Levyra usa un workflow stabile per controllare la dimensione di APK e AAB debug.

Il task Ruler diretto è stato disattivato perché nella combinazione AGP/Gradle attuale fallisce internamente su analyzeDebugBundle.

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
