[CmdletBinding()]
param(
    [switch] $DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$claudeMemPackage = 'claude-mem@13.15.0'
$claudeMemModel = 'claude-haiku-4-5-20251001'
$isWindowsPlatform = [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT

function Test-Command {
    param([Parameter(Mandatory)][string] $Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-ClaudeMem {
    param(
        [Parameter(Mandatory)][string] $Label,
        [Parameter(Mandatory)][string[]] $Arguments,
        [switch] $AllowFailure
    )

    $display = "npx --yes $claudeMemPackage " + ($Arguments -join ' ')
    if ($DryRun) {
        Write-Host "[dry-run] $Label`: $display"
        return $true
    }

    Write-Host "[run] $Label"
    $global:LASTEXITCODE = 0
    '' | & npx --yes $claudeMemPackage @Arguments 2>&1 | ForEach-Object { Write-Host $_ }
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0) {
        return $true
    }

    $message = "$Label failed with exit code $exitCode"
    if ($AllowFailure) {
        Write-Warning $message
        return $false
    }

    throw $message
}

function Set-WindowsFailOpenGuard {
    if (-not $isWindowsPlatform) {
        return
    }

    $settingsPath = Join-Path $HOME '.claude-mem/settings.json'
    if ($DryRun) {
        Write-Output "[dry-run] Keep claude-mem hooks fail-open on Windows via CLAUDE_MEM_HOOK_FAIL_LOUD_THRESHOLD=999999999"
        return
    }
    if (-not (Test-Path -LiteralPath $settingsPath -PathType Leaf)) {
        Write-Warning "claude-mem settings not found at $settingsPath; Windows fail-open guard was not applied."
        return
    }

    try {
        $settings = Get-Content -LiteralPath $settingsPath -Raw | ConvertFrom-Json
        $settings | Add-Member -NotePropertyName 'CLAUDE_MEM_HOOK_FAIL_LOUD_THRESHOLD' -NotePropertyValue '999999999' -Force
        $json = $settings | ConvertTo-Json -Depth 32
        Set-Content -LiteralPath $settingsPath -Value $json -Encoding utf8
        Write-Output '[ok] Applied Windows fail-open guard for claude-mem hooks.'
    }
    catch {
        Write-Warning "Could not apply the Windows fail-open guard: $($_.Exception.Message)"
    }
}

Write-Output 'Levyra claude-mem setup'
Write-Output "Package: $claudeMemPackage"

if (-not (Test-Command 'npx')) {
    throw 'npx is required for claude-mem. Install Node.js/npm, then rerun this script.'
}

$ides = [System.Collections.Generic.List[string]]::new()

if (Test-Command 'claude') {
    $ides.Add('claude-code')
}
if ((Test-Command 'codex') -or (Test-Path -LiteralPath (Join-Path $HOME '.codex'))) {
    $ides.Add('codex-cli')
}
if ((Test-Command 'agy') -or (Test-Path -LiteralPath (Join-Path $HOME '.gemini/antigravity'))) {
    $ides.Add('antigravity')
}

if ($ides.Count -eq 0) {
    Write-Output '[skip] No local Claude Code, Codex CLI, or Antigravity installation was detected.'
    Write-Output 'ChatGPT uses claude-mem only through a separately connected MCP app; see docs/ai/CLAUDE_MEM.md.'
    return
}

$failed = [System.Collections.Generic.List[string]]::new()

foreach ($ide in $ides) {
    $ok = Invoke-ClaudeMem "Install claude-mem for $ide" @(
        'install',
        '--ide', $ide,
        '--provider', 'claude',
        '--model', $claudeMemModel,
        '--runtime', 'worker',
        '--no-auto-start'
    ) -AllowFailure

    if (-not $ok) {
        $failed.Add($ide)
    }
}

if ($failed.Count -eq $ides.Count) {
    throw "claude-mem failed for every detected runtime: $($failed -join ', ')"
}

Invoke-ClaudeMem 'Disable claude-mem anonymous telemetry' @('telemetry', 'disable') -AllowFailure | Out-Null
Set-WindowsFailOpenGuard

$started = Invoke-ClaudeMem 'Start claude-mem worker' @('start') -AllowFailure
$healthy = Invoke-ClaudeMem 'Check claude-mem health' @('doctor') -AllowFailure

if (-not $healthy) {
    Write-Warning 'claude-mem health check failed. Running one official repair attempt.'
    $repaired = Invoke-ClaudeMem 'Repair claude-mem runtime' @('repair') -AllowFailure
    if ($repaired) {
        Invoke-ClaudeMem 'Restart claude-mem worker after repair' @('start') -AllowFailure | Out-Null
        $healthy = Invoke-ClaudeMem 'Re-check claude-mem health' @('doctor') -AllowFailure
    }
}

Write-Output ''
if ($healthy) {
    Write-Output '[ok] claude-mem is installed and healthy.'
}
else {
    Write-Warning 'claude-mem remains unhealthy. Levyra agents must continue without memory instead of blocking work.'
}

if ($failed.Count -gt 0) {
    throw "claude-mem is healthy for at least one runtime, but integration failed for: $($failed -join ', ')"
}
if (-not $healthy) {
    throw 'claude-mem setup completed partially but the worker health check is still failing.'
}

Write-Output "Configured runtimes: $($ides -join ', ')"
Write-Output 'Cloud sync was not enabled. Experimental semantic injection was not enabled.'
Write-Output 'Start a new coding-agent conversation so hooks and MCP tools are reloaded.'
