<#
.SYNOPSIS
    Pulls the Levyra issue #427 playback-memory diagnostics CSV from one explicit device.

.EXAMPLE
    .\scripts\pull-issue-427-diagnostics.ps1 -Serial 1A2B3C4D
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z0-9._:-]+$')]
    [string]$Serial,

    [ValidatePattern('^[A-Za-z0-9._]+$')]
    [string]$Package,

    [string]$OutputPath = (Join-Path (Get-Location) 'issue-427-playback-memory.csv')
)

$ErrorActionPreference = 'Stop'

$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
$adb = if ($sdkRoot) { Join-Path $sdkRoot 'platform-tools\adb.exe' } else { 'adb' }
if (-not (Get-Command $adb -ErrorAction SilentlyContinue)) {
    throw "adb was not found. Install Android platform-tools or set ANDROID_HOME, then retry."
}

$devices = & $adb devices
if ($LASTEXITCODE -ne 0) { throw "Unable to query connected devices with adb." }
if (-not ($devices | Select-String -SimpleMatch "$Serial`tdevice")) {
    throw "Device '$Serial' is not connected and authorized. Connected devices:`n$($devices -join [Environment]::NewLine)"
}

$candidates = if ($Package) { @($Package) } else { @('com.luc4n3x.levyra', 'com.luc4n3x.levyra.debug') }
$relativePath = 'files/diagnostics/issue-427-playback-memory.csv'

$pulled = $false
foreach ($candidate in $candidates) {
    $remotePath = "/sdcard/Android/data/$candidate/$relativePath"
    & $adb -s $Serial pull $remotePath $OutputPath 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0 -and (Test-Path $OutputPath)) { $pulled = $true; break }
}

if (-not $pulled) {
    throw "No Levyra issue #427 diagnostics found on $Serial for: $($candidates -join ', '). Play music in Levyra for a few minutes first, then retry."
}

$rows = (Get-Content $OutputPath | Measure-Object -Line).Lines - 1
if ($rows -lt 1) {
    throw "The diagnostics file on $Serial is empty. Play music in Levyra for a few minutes first, then retry."
}

Write-Output "Saved Levyra issue #427 diagnostics to $OutputPath"
Write-Output "Samples: $rows (about $([math]::Round($rows * 5 / 60.0, 1)) minutes of playback)"
Write-Output "Attach this file to https://github.com/LUC4N3X/Levyra-deepsound/issues/427"
