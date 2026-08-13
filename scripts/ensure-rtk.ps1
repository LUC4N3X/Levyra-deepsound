[CmdletBinding()]
param(
    [switch] $DryRun,
    [switch] $Quiet
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$rtkGitRevision = 'b34be37caf3796b69a50952a28e60e32b5daad43'

function Test-Command {
    param([Parameter(Mandatory)][string] $Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-RtkReady {
    if (-not (Test-Command 'rtk')) {
        return $false
    }

    $global:LASTEXITCODE = 0
    & rtk --version *> $null
    if ($LASTEXITCODE -ne 0) {
        return $false
    }

    $global:LASTEXITCODE = 0
    & rtk gain *> $null
    return $LASTEXITCODE -eq 0
}

function Write-Step {
    param([Parameter(Mandatory)][string] $Message)
    if (-not $Quiet) {
        Write-Output $Message
    }
}

if (Test-RtkReady) {
    Write-Step '[ok] Levyra RTK is ready.'
    exit 0
}

if (-not (Test-Command 'cargo')) {
    Write-Warning 'Levyra RTK is unavailable and Cargo is not installed; continuing without RTK.'
    exit 1
}

if ($DryRun) {
    Write-Output "[dry-run] cargo install --git https://github.com/rtk-ai/rtk --rev $rtkGitRevision --force"
    exit 0
}

Write-Step '[run] Installing the owner-authorized pinned RTK build for Levyra.'
$global:LASTEXITCODE = 0
& cargo install --git https://github.com/rtk-ai/rtk --rev $rtkGitRevision --force
if ($LASTEXITCODE -ne 0) {
    Write-Warning "RTK installation failed with exit code $LASTEXITCODE; continuing without RTK."
    exit 1
}

if (-not (Test-RtkReady)) {
    Write-Warning 'RTK installed but validation failed; continuing without RTK.'
    exit 1
}

Write-Step '[ok] Levyra RTK installed and verified.'
exit 0
