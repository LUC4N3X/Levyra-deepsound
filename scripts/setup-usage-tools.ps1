[CmdletBinding()]
param(
    [switch] $DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$toolRoot = Join-Path $repoRoot '.levyra-tools'
$headroomPrefix = Join-Path $toolRoot 'headroom'
$headroomExe = Join-Path $headroomPrefix 'headroom.exe'
$headroomVersion = 'v0.3.0'
$codeBurnVersion = '0.9.24'
$headroomInstallerUrl = "https://raw.githubusercontent.com/anthonybo/headroom/$headroomVersion/install.ps1"

function Test-Command {
    param([Parameter(Mandatory)][string] $Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-HeadroomBinary {
    if (-not (Test-Path -LiteralPath $headroomExe -PathType Leaf)) {
        return $false
    }

    try {
        $global:LASTEXITCODE = 0
        & $headroomExe --version
        return $LASTEXITCODE -eq 0
    }
    catch {
        return $false
    }
}

Write-Output 'Levyra AI usage tooling setup'
Write-Output "Repository: $repoRoot"

if ($DryRun) {
    Write-Output "[dry-run] Install Headroom $headroomVersion into $headroomPrefix without changing global Claude settings or PATH"
}
elseif (Test-HeadroomBinary) {
    Write-Output "[ok] Headroom $headroomVersion already installed: $headroomExe"
}
else {
    $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("levyra-headroom-" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    try {
        $installerPath = Join-Path $tempDir 'install.ps1'
        Invoke-WebRequest -Uri $headroomInstallerUrl -OutFile $installerPath -UseBasicParsing

        $installerError = $null
        $installerExitCode = 0
        try {
            $global:LASTEXITCODE = 0
            & $installerPath -NoWire -NoPath -Version $headroomVersion -Prefix $headroomPrefix
            $installerExitCode = $LASTEXITCODE
        }
        catch {
            $installerError = $_.Exception.Message
            $installerExitCode = $LASTEXITCODE
        }

        if (-not (Test-HeadroomBinary)) {
            if ($installerError) {
                throw "Headroom installer failed and the installed binary did not verify: $installerError"
            }
            throw "Headroom installer failed with exit code $installerExitCode and the installed binary did not verify"
        }

        if ($installerError -or $installerExitCode -ne 0) {
            $detail = if ($installerError) { $installerError } else { "exit code $installerExitCode" }
            Write-Warning "Headroom installer reported an error after writing the binary, but the binary passed verification. Continuing. Details: $detail"
        }
    }
    finally {
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if (-not (Test-Command 'npx')) {
    Write-Warning "CodeBurn $codeBurnVersion requires Node.js 22.13+ with npm/npx. Headroom setup can still be used."
}
elseif ($DryRun) {
    Write-Output "[dry-run] Verify CodeBurn $codeBurnVersion through npx without installing it globally"
}
else {
    & npx -y "codeburn@$codeBurnVersion" --version
    if ($LASTEXITCODE -ne 0) {
        throw "CodeBurn verification failed with exit code $LASTEXITCODE"
    }
}

Write-Output ''
Write-Output 'Setup complete.'
Write-Output 'Headroom is project-local under .levyra-tools/ and is wired by the tracked Claude statusLine configuration.'
Write-Output 'CodeBurn stays uninstalled globally and is invoked through scripts/codeburn-levyra.ps1.'
