[CmdletBinding()]
param(
    [string]$DeviceId = "",
    [string]$ApkPath = "",
    [string]$PackageName = "com.luc4n3x.levyra.debug",
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$ExercisePlayback,
    [switch]$RequirePlayback,
    [ValidateRange(1, 20)]
    [int]$ColdStartRuns = 5,
    [ValidateRange(1000, 60000)]
    [int]$PlaybackPollTimeoutMs = 10000,
    [string]$ReportDirectory = ".report/device-qualification"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$mainActivity = "com.luc4n3x.levyra.MainActivity"
$selectedDevice = ""
$reportRoot = if ([System.IO.Path]::IsPathRooted($ReportDirectory)) {
    $ReportDirectory
} else {
    Join-Path $repoRoot $ReportDirectory
}
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportPath = Join-Path $reportRoot "device-qualification-$timestamp.json"
$logcatPath = Join-Path $reportRoot "logcat-$timestamp.txt"
$mediaSessionPath = Join-Path $reportRoot "media-session-$timestamp.txt"
$meminfoPath = Join-Path $reportRoot "meminfo-$timestamp.txt"

New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Args,
        [switch]$AllowFailure
    )

    $output = & adb -s $script:selectedDevice @Args 2>&1
    $exitCode = $LASTEXITCODE
    $lines = @($output | ForEach-Object { $_.ToString() })
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "adb $($Args -join ' ') failed with exit code $exitCode`n$($lines -join "`n")"
    }
    return $lines
}

function Resolve-Device {
    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb was not found in PATH."
    }

    $rows = @(& adb devices 2>&1 | ForEach-Object { $_.ToString() })
    if ($LASTEXITCODE -ne 0) {
        throw "adb devices failed.`n$($rows -join "`n")"
    }

    $devices = @(
        $rows |
            Select-Object -Skip 1 |
            ForEach-Object {
                if ($_ -match "^\s*(\S+)\s+device(?:\s|$)") {
                    $Matches[1]
                }
            } |
            Where-Object { $_ }
    )

    if ($DeviceId) {
        if ($DeviceId -notin $devices) {
            throw "Requested device '$DeviceId' is not connected and authorized. Connected devices: $($devices -join ', ')"
        }
        return $DeviceId
    }

    if ($devices.Count -eq 0) {
        throw "No authorized Android device is connected."
    }
    if ($devices.Count -gt 1) {
        throw "Multiple Android devices are connected. Re-run with -DeviceId. Connected devices: $($devices -join ', ')"
    }
    return $devices[0]
}

function Invoke-GradleDebugBuild {
    $wrapper = Join-Path $repoRoot "gradlew.bat"
    if (-not (Test-Path $wrapper)) {
        throw "Gradle wrapper not found: $wrapper"
    }

    Push-Location $repoRoot
    try {
        & $wrapper --no-daemon :app:assembleDebug
        if ($LASTEXITCODE -ne 0) {
            throw "Debug build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Resolve-Apk {
    if ($ApkPath) {
        $candidate = if ([System.IO.Path]::IsPathRooted($ApkPath)) {
            $ApkPath
        } else {
            Join-Path $repoRoot $ApkPath
        }
        if (-not (Test-Path $candidate)) {
            throw "APK not found: $candidate"
        }
        return (Resolve-Path $candidate).Path
    }

    $debugDir = Join-Path $repoRoot "app/build/outputs/apk/debug"
    $apk = Get-ChildItem -Path $debugDir -Filter *.apk -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($null -eq $apk) {
        throw "No debug APK found under $debugDir."
    }
    return $apk.FullName
}

function Install-Apk {
    param([Parameter(Mandatory = $true)][string]$Path)

    $output = & adb -s $script:selectedDevice install -r $Path 2>&1
    $exitCode = $LASTEXITCODE
    $text = @($output | ForEach-Object { $_.ToString() })
    if ($exitCode -ne 0 -or -not (($text -join "`n") -match "(?m)^Success$")) {
        throw "APK install failed with exit code $exitCode.`n$($text -join "`n")"
    }
}

function Ensure-InteractiveDevice {
    Invoke-Adb -Args @("shell", "input", "keyevent", "KEYCODE_WAKEUP") -AllowFailure | Out-Null
    Invoke-Adb -Args @("shell", "wm", "dismiss-keyguard") -AllowFailure | Out-Null
    Start-Sleep -Milliseconds 500
}

function Get-Prop {
    param([Parameter(Mandatory = $true)][string]$Name)

    return ((Invoke-Adb -Args @("shell", "getprop", $Name) -AllowFailure) -join "").Trim()
}

function Parse-AmStartTiming {
    param([Parameter(Mandatory = $true)][string[]]$Lines)

    $text = $Lines -join "`n"
    $values = @{}
    foreach ($key in @("ThisTime", "TotalTime", "WaitTime")) {
        $match = [regex]::Match($text, "(?m)^${key}:\s*(\d+)\s*$")
        $values[$key] = if ($match.Success) { [int]$match.Groups[1].Value } else { $null }
    }
    return [pscustomobject]@{
        status = if ($text -match "(?m)^Status:\s*ok\s*$") { "ok" } else { "unknown" }
        launchState = ([regex]::Match($text, "(?m)^LaunchState:\s*(\S+)\s*$")).Groups[1].Value
        thisTimeMs = $values["ThisTime"]
        totalTimeMs = $values["TotalTime"]
        waitTimeMs = $values["WaitTime"]
        raw = $text
    }
}

function Measure-ColdStart {
    Invoke-Adb -Args @("shell", "am", "force-stop", $PackageName) | Out-Null
    Start-Sleep -Milliseconds 400
    Ensure-InteractiveDevice

    $output = Invoke-Adb -Args @(
        "shell", "am", "start", "-W",
        "-n", "$PackageName/$mainActivity"
    )
    return Parse-AmStartTiming -Lines $output
}

function Get-PackageInfo {
    $dump = (Invoke-Adb -Args @("shell", "dumpsys", "package", $PackageName)) -join "`n"
    $versionNameMatch = [regex]::Match($dump, "(?m)^\s*versionName=(.+?)\s*$")
    $versionCodeMatch = [regex]::Match($dump, "(?m)^\s*versionCode=(\d+)")
    return [pscustomobject]@{
        versionName = if ($versionNameMatch.Success) { $versionNameMatch.Groups[1].Value.Trim() } else { "" }
        versionCode = if ($versionCodeMatch.Success) { [long]$versionCodeMatch.Groups[1].Value } else { $null }
    }
}

function Get-MediaSessionSnapshot {
    $dump = (Invoke-Adb -Args @("shell", "dumpsys", "media_session") -AllowFailure) -join "`n"
    $packageToken = "package=$PackageName"
    $index = $dump.IndexOf($packageToken, [System.StringComparison]::OrdinalIgnoreCase)
    if ($index -lt 0) {
        return [pscustomobject]@{
            found = $false
            state = ""
            stateCode = $null
            positionMs = $null
            raw = $dump
        }
    }

    $length = [Math]::Min(7000, $dump.Length - $index)
    $segment = $dump.Substring($index, $length)
    $nextSession = $segment.IndexOf("Session #", 1, [System.StringComparison]::OrdinalIgnoreCase)
    if ($nextSession -gt 0) {
        $segment = $segment.Substring(0, $nextSession)
    }

    $stateMatch = [regex]::Match($segment, "state=PlaybackState\s*\{state=([A-Z_]+)\((\d+)\)")
    $positionMatch = [regex]::Match($segment, "position=(\d+)")
    return [pscustomobject]@{
        found = $true
        state = if ($stateMatch.Success) { $stateMatch.Groups[1].Value } else { "" }
        stateCode = if ($stateMatch.Success) { [int]$stateMatch.Groups[2].Value } else { $null }
        positionMs = if ($positionMatch.Success) { [long]$positionMatch.Groups[1].Value } else { $null }
        raw = $dump
    }
}

function Exercise-PlaybackSession {
    $before = Get-MediaSessionSnapshot
    if (-not $before.found) {
        return [pscustomobject]@{
            requested = $true
            sessionFound = $false
            initialState = ""
            reachedPlaying = $false
            playingLatencyMs = $null
            cleanupPauseSent = $false
            finalState = ""
        }
    }

    if ($before.state -eq "PLAYING") {
        return [pscustomobject]@{
            requested = $true
            sessionFound = $true
            initialState = $before.state
            reachedPlaying = $true
            playingLatencyMs = 0
            cleanupPauseSent = $false
            finalState = $before.state
        }
    }

    $startedAt = Get-Date
    Invoke-Adb -Args @("shell", "input", "keyevent", "KEYCODE_MEDIA_PLAY") -AllowFailure | Out-Null
    $reachedPlaying = $false
    $playingLatencyMs = $null
    $pollCount = [Math]::Max(1, [Math]::Ceiling($PlaybackPollTimeoutMs / 250.0))

    for ($index = 0; $index -lt $pollCount; $index++) {
        Start-Sleep -Milliseconds 250
        $snapshot = Get-MediaSessionSnapshot
        if ($snapshot.found -and $snapshot.state -eq "PLAYING") {
            $reachedPlaying = $true
            $playingLatencyMs = [int]((Get-Date) - $startedAt).TotalMilliseconds
            break
        }
    }

    $cleanupPauseSent = $false
    if ($reachedPlaying) {
        Invoke-Adb -Args @("shell", "input", "keyevent", "KEYCODE_MEDIA_PAUSE") -AllowFailure | Out-Null
        Start-Sleep -Milliseconds 300
        $cleanupPauseSent = $true
    }

    $final = Get-MediaSessionSnapshot
    return [pscustomobject]@{
        requested = $true
        sessionFound = $true
        initialState = $before.state
        reachedPlaying = $reachedPlaying
        playingLatencyMs = $playingLatencyMs
        cleanupPauseSent = $cleanupPauseSent
        finalState = $final.state
    }
}

function Get-Percentile {
    param(
        [Parameter(Mandatory = $true)][int[]]$Values,
        [Parameter(Mandatory = $true)][double]$Percentile
    )

    if ($Values.Count -eq 0) {
        return $null
    }
    $sorted = @($Values | Sort-Object)
    $index = [Math]::Ceiling($Percentile * $sorted.Count) - 1
    $index = [Math]::Max(0, [Math]::Min($sorted.Count - 1, $index))
    return $sorted[$index]
}

function Get-GitSha {
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        return ""
    }
    $output = & git -C $repoRoot rev-parse HEAD 2>$null
    if ($LASTEXITCODE -ne 0) {
        return ""
    }
    return ($output | Select-Object -First 1).ToString().Trim()
}

$selectedDevice = Resolve-Device
Ensure-InteractiveDevice

if (-not $SkipBuild) {
    Invoke-GradleDebugBuild
}

$resolvedApk = Resolve-Apk

if (-not $SkipInstall) {
    Install-Apk -Path $resolvedApk
}

$packageInfo = Get-PackageInfo
Invoke-Adb -Args @("logcat", "-c") -AllowFailure | Out-Null

$coldStarts = @()
for ($run = 1; $run -le $ColdStartRuns; $run++) {
    $measurement = Measure-ColdStart
    $coldStarts += [pscustomobject]@{
        run = $run
        status = $measurement.status
        launchState = $measurement.launchState
        thisTimeMs = $measurement.thisTimeMs
        totalTimeMs = $measurement.totalTimeMs
        waitTimeMs = $measurement.waitTimeMs
    }
}

$successfulTotalTimes = @(
    $coldStarts |
        Where-Object { $_.status -eq "ok" -and $null -ne $_.totalTimeMs } |
        ForEach-Object { [int]$_.totalTimeMs }
)
$averageTotalTimeMs = if ($successfulTotalTimes.Count -gt 0) {
    [Math]::Round(($successfulTotalTimes | Measure-Object -Average).Average, 1)
} else {
    $null
}
$p50TotalTimeMs = Get-Percentile -Values $successfulTotalTimes -Percentile 0.50
$p95TotalTimeMs = Get-Percentile -Values $successfulTotalTimes -Percentile 0.95

$initialSession = Get-MediaSessionSnapshot
$playback = if ($ExercisePlayback) {
    Exercise-PlaybackSession
} else {
    [pscustomobject]@{
        requested = $false
        sessionFound = $initialSession.found
        initialState = $initialSession.state
        reachedPlaying = $initialSession.state -eq "PLAYING"
        playingLatencyMs = if ($initialSession.state -eq "PLAYING") { 0 } else { $null }
        cleanupPauseSent = $false
        finalState = $initialSession.state
    }
}

$mediaSessionDump = (Get-MediaSessionSnapshot).raw
Set-Content -Path $mediaSessionPath -Value $mediaSessionDump -Encoding utf8

$meminfo = (Invoke-Adb -Args @("shell", "dumpsys", "meminfo", $PackageName) -AllowFailure) -join "`n"
Set-Content -Path $meminfoPath -Value $meminfo -Encoding utf8
$totalPssMatch = [regex]::Match($meminfo, "(?m)\bTOTAL PSS:\s*([\d,]+)")
$totalRssMatch = [regex]::Match($meminfo, "(?m)\bTOTAL RSS:\s*([\d,]+)")
$totalPssKb = if ($totalPssMatch.Success) { [long]($totalPssMatch.Groups[1].Value -replace ",", "") } else { $null }
$totalRssKb = if ($totalRssMatch.Success) { [long]($totalRssMatch.Groups[1].Value -replace ",", "") } else { $null }

$logcat = (Invoke-Adb -Args @("logcat", "-d", "-v", "threadtime") -AllowFailure) -join "`n"
Set-Content -Path $logcatPath -Value $logcat -Encoding utf8
$crashLines = @(
    $logcat -split "`n" |
        Where-Object {
            $_ -match "ANR in $([regex]::Escape($PackageName))(?:\s|$)" -or
            $_ -match "Process:\s*$([regex]::Escape($PackageName))(?:,|\s|$)" -or
            $_ -match "Cmdline:\s*$([regex]::Escape($PackageName))(?:\s|$)"
        } |
        Select-Object -First 80
)

$coldStartPass = $coldStarts.Count -eq $ColdStartRuns -and @($coldStarts | Where-Object { $_.status -ne "ok" }).Count -eq 0
$playbackPass = -not $RequirePlayback -or $playback.reachedPlaying
$crashPass = $crashLines.Count -eq 0
$overallPass = $coldStartPass -and $playbackPass -and $crashPass

$report = [ordered]@{
    schema = "levyra-device-qualification-v1"
    createdAt = (Get-Date).ToString("o")
    status = if ($overallPass) { "PASS" } else { "FAIL" }
    repository = [ordered]@{
        gitSha = Get-GitSha
        apkPath = $resolvedApk
        packageName = $PackageName
        versionName = $packageInfo.versionName
        versionCode = $packageInfo.versionCode
    }
    device = [ordered]@{
        serial = $selectedDevice
        manufacturer = Get-Prop -Name "ro.product.manufacturer"
        model = Get-Prop -Name "ro.product.model"
        androidRelease = Get-Prop -Name "ro.build.version.release"
        sdk = Get-Prop -Name "ro.build.version.sdk"
        fingerprint = Get-Prop -Name "ro.build.fingerprint"
    }
    coldStart = [ordered]@{
        requestedRuns = $ColdStartRuns
        passed = $coldStartPass
        averageTotalTimeMs = $averageTotalTimeMs
        p50TotalTimeMs = $p50TotalTimeMs
        p95TotalTimeMs = $p95TotalTimeMs
        runs = $coldStarts
    }
    playback = $playback
    memory = [ordered]@{
        totalPssKb = $totalPssKb
        totalRssKb = $totalRssKb
        rawReport = $meminfoPath
    }
    diagnostics = [ordered]@{
        crashSignalsFound = $crashLines.Count
        crashSignals = $crashLines
        logcat = $logcatPath
        mediaSession = $mediaSessionPath
    }
}

$report | ConvertTo-Json -Depth 8 | Set-Content -Path $reportPath -Encoding utf8

Write-Output ""
Write-Output "Levyra device qualification: $($report.status)"
Write-Output "Device: $($report.device.manufacturer) $($report.device.model) (Android $($report.device.androidRelease), SDK $($report.device.sdk))"
Write-Output "Cold start: avg=$averageTotalTimeMs ms p50=$p50TotalTimeMs ms p95=$p95TotalTimeMs ms"
Write-Output "Playback: requested=$($playback.requested) session=$($playback.sessionFound) reachedPlaying=$($playback.reachedPlaying) latency=$($playback.playingLatencyMs) ms"
Write-Output "Memory: PSS=$totalPssKb KB RSS=$totalRssKb KB"
Write-Output "Crash signals: $($crashLines.Count)"
Write-Output "Report: $reportPath"

if (-not $overallPass) {
    exit 1
}
