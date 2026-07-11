# LevyraExtractor integration

Levyra builds LevyraExtractor directly from the local Gradle modules included in this repository.

```text
:app
  -> :extractor
       -> :timeago-parser
```

The application dependency is declared as:

```kotlin
implementation(project(":extractor"))
```

This removes the LevyraExtractor JitPack artifact from the application build path. GitHub Actions compiles the two extractor modules before assembling the application, so a missing LevyraExtractor POM cannot block the APK build.

The source remains under GPL-3.0 and retains the upstream NewPipe, PipePipe and Metrolist attribution present in the repository notices.
