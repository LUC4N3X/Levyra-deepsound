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

### Physical-device qualification

On Windows, the repository includes a repeatable ADB qualification harness for a connected Android phone:

```powershell
.\scripts\levyra-device-qualification.ps1
```

The harness builds and installs the current debug APK unless asked not to, performs repeated cold starts, inspects Levyra's MediaSession, captures process memory, and stores focused logcat and diagnostic dumps. Target selection follows a fixed fallback order: an authorized USB device first, then Android Wireless Debugging, then a running Android emulator. If no USB target is available, the harness also tries to reconnect one already-paired Wireless Debugging device discovered through ADB mDNS before falling back to the emulator.

Wireless pairing remains an Android/ADB setup step; the harness never handles pairing codes or stores pairing secrets. If more than one target exists at the selected priority, select the target explicitly:

```powershell
.\scripts\levyra-device-qualification.ps1 -DeviceId <serial>
```

To exercise an existing or restored Levyra playback session and require it to reach `PLAYING`:

```powershell
.\scripts\levyra-device-qualification.ps1 -ExercisePlayback -RequirePlayback
```

Playback exercise is intentionally limited to an already available Levyra MediaSession and queue. The harness does not add a debug-only playback entry point, bypass onboarding, inject credentials, or depend on a private account. If it starts a paused session, it sends pause again before finishing.

For fast reruns after a local build and install:

```powershell
.\scripts\levyra-device-qualification.ps1 -SkipBuild -SkipInstall
```

Reports are written under:

```text
.report/device-qualification/
```

Each run produces a JSON summary plus raw local Logcat, MediaSession, and `dumpsys meminfo` evidence. `.report/` is ignored by Git.

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
