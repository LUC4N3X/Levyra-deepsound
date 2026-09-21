# Development

Levyra is built as two native applications:

- **Android**: Kotlin, Jetpack Compose, and AndroidX Media3 / ExoPlayer.
- **Windows**: Kotlin, Compose Multiplatform, and libvlc.

## Clone the repository

```bash
git clone https://github.com/LUC4N3X/Levyra-deepsound.git
cd Levyra-deepsound
```

## Android

### Requirements

- JDK 17
- Android SDK Platform 37
- Gradle 9.7.0 (using the repository wrapper)

### Debug build

=== "Linux / macOS"

    ```bash
    ./gradlew assembleDebug
    ```

=== "Windows"

    ```powershell
    .\gradlew.bat assembleDebug
    ```

Debug APK outputs are located at:

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

Verify your device connection via ADB:

```bash
adb devices
```

### Physical device qualification

On Windows, the repository includes an automated ADB test harness for testing on a connected phone:

```powershell
.\scripts\levyra-device-qualification.ps1
```

The script builds and installs the debug APK, runs cold start measurements, checks the MediaSession state, inspects memory usage, and collects diagnostic logs. Target devices are selected in this order: an authorized USB device, a wireless debugging device, or a running Android emulator.

If you have multiple devices connected, specify the target serial number directly:

```powershell
.\scripts\levyra-device-qualification.ps1 -DeviceId <serial>
```

To test playback from an existing session and ensure it reaches the active playing state:

```powershell
.\scripts\levyra-device-qualification.ps1 -ExercisePlayback -RequirePlayback
```

To rerun tests quickly without rebuilding or reinstalling:

```powershell
.\scripts\levyra-device-qualification.ps1 -SkipBuild -SkipInstall
```

Diagnostic reports are saved locally to:

```text
.report/device-qualification/
```

Each run generates a JSON summary along with raw Logcat output, MediaSession state dumps, and `dumpsys meminfo` metrics.

### Release build

=== "Linux / macOS"

    ```bash
    ./gradlew clean assembleRelease
    ```

=== "Windows"

    ```powershell
    .\gradlew.bat clean assembleRelease
    ```

Release builds require a valid signing configuration.

## Windows Desktop

### Requirements

- Windows 10 or 11 (x64)
- JDK 21 LTS
- VLC 3.0.x / libvlc runtime
- WiX Toolset 3.14 (for MSI installers)

### Building packages

```powershell
cd desktop
.\gradlew.bat check
.\gradlew.bat createReleaseDistributable
.\gradlew.bat packageReleaseMsi packageReleaseExe
```

Desktop binaries and installers are output to:

```text
desktop/app/build/compose/binaries/main-release/
```

## Repository map

```text
app/                 Android client
desktop/             Windows Desktop client
baselineprofile/     Android baseline profile definitions
levyra-recognition/  Audio recognition module
docs/                Project documentation and website
scripts/             Build, CI, and validation tools
```

## Contribution workflow

1. Create a branch from `main`.
2. Keep your changes focused on a single issue or feature.
3. Respect existing architecture, threading rules, and UI conventions.
4. Run targeted checks before submitting.
5. Check against the repository quality gate:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing or opening a pull request, run the complete gate:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

For complete engineering guidelines and coding standards, review [AGENTS.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/AGENTS.md).
