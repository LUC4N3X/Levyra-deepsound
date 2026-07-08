# APK Size Report

Levyra uses a GitHub Actions workflow only to report debug APK and AAB size.

No hard limits are enforced for downloads, install size, APK size, or AAB size.

The direct Spotify Ruler task `:app:analyzeDebugBundle` is intentionally disabled because the current AGP/Gradle combination fails internally with an `ApplicationExtensionImpl` to `BaseExtension` cast error.

The workflow now runs:

```text
:app:bundleDebug
:app:assembleDebug
```

Then it generates:

```text
build/size-report/apk-size-report.md
build/size-report/apk-size-report.json
```

The report is visibility-only and does not block pull requests based on generated package size.
