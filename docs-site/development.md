# Development

Levyra contains two native application targets:

- **Android** — Kotlin, Jetpack Compose and AndroidX Media3 / ExoPlayer.
- **Windows** — Kotlin, Compose Multiplatform and libvlc.

## Clone the repository

```bash
git clone https://github.com/LUC4N3X/Levyra-deepsound.git
cd Levyra-deepsound
```

## Android

### Requirements

- JDK 17
- Android SDK Platform 37
- Gradle 9.7.0 through the repository wrapper

### Debug build

=== "Linux / macOS"

    ```bash
    ./gradlew assembleDebug
    ```

=== "Windows"

    ```powershell
    .\gradlew.bat assembleDebug
    ```

Debug APKs are written under:

```text
app/build/outputs/apk/debug/
```

### Install on a connected device

=== "Linux / macOS"

    ```bash
    ./gradlew installDebug
    ```

=== "Windows"

    ```powershell
    .\gradlew.bat installDebug
    ```

Verify ADB first when needed:

```bash
adb devices
```

### Release compile

=== "Linux / macOS"

    ```bash
    ./gradlew clean assembleRelease
    ```

=== "Windows"

    ```powershell
    .\gradlew.bat clean assembleRelease
    ```

Release signing requirements differ from local debug builds.

## Windows Desktop

### Requirements

- Windows x64
- JDK 21 LTS
- VLC 3.0.x / libvlc
- WiX Toolset 3.14

```powershell
cd desktop
.\gradlew.bat check
.\gradlew.bat createReleaseDistributable
.\gradlew.bat packageReleaseMsi packageReleaseExe
```

Desktop release artifacts are produced under:

```text
desktop/app/build/compose/binaries/main-release/
```

## Repository map

```text
app/                 Android client
desktop/             Windows client
baselineprofile/     Android baseline-profile support
levyra-recognition/  Recognition-related module
docs/                Repository documentation
scripts/             Validation and project tooling
```

## Contribution workflow

1. Branch from the latest `main`.
2. Keep the change focused.
3. Preserve current architecture and user-visible behavior outside the intended scope.
4. Run the narrowest useful validation.
5. Run Levyra's repository quality gate before publication.
6. Open a focused pull request with truthful test evidence.

The repository quality gate is:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before push or PR publication, repository instructions require the full profile:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

For the complete engineering contract, read [AGENTS.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/AGENTS.md).
