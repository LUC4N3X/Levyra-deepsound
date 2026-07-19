---
paths:
  - "app/src/test/**/*.kt"
  - "app/src/androidTest/**/*.kt"
  - ".github/workflows/**/*.yml"
  - "app/build.gradle.kts"
  - "build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle.properties"
  - "gradle/libs.versions.toml"
---

# Testing, CI, and Release

- Use `./gradlew` and the repository's pinned wrapper version.
- Start with the narrowest affected unit test, then run the applicable broader checks.
- The primary CI reference is `.github/workflows/pr-check.yml`; mirror its environment and temporary signing strategy instead of inventing another workflow.
- Release tasks require `YOUTUBE_INNERTUBE_API_KEY` and signing inputs. Never commit real values to satisfy them.
- Keep scheduled workflow times in UTC and state the corresponding local time carefully.
- A successful workflow with no changed upstream config may correctly produce no PR.
- Do not duplicate release, APK, size, extractor, config-sync, or workflow-guard automation.
- Do not update app version values outside explicit release work.
- Run `git diff --check`; inspect for conflict markers, HTML accidentally pasted into source, generated binaries, secrets, and unrelated formatting churn.
- Mark manual checks such as device installation, playback, downloads, Android Auto, and language switching as complete only after they were actually performed.
