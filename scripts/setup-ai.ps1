[CmdletBinding()]
param(
    [switch] $DryRun,
    [switch] $InstallRtk,
    [switch] $Plugins,
    [switch] $SkipHooks
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$pluginManifest = Join-Path $repoRoot 'codex-plugins.txt'

function Test-Command {
    param([Parameter(Mandatory)][string] $Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-SetupCommand {
    param(
        [Parameter(Mandatory)][string] $Label,
        [Parameter(Mandatory)][scriptblock] $Command
    )

    if ($DryRun) {
        Write-Output "[dry-run] $Label"
        return
    }

    Write-Output "[run] $Label"
    $global:LASTEXITCODE = 0
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE"
    }
}

Write-Output "Levyra AI efficiency setup"
Write-Output "Repository: $repoRoot"

if (-not (Test-Command 'rtk')) {
    if (-not $InstallRtk) {
        Write-Warning 'RTK is not installed. Re-run with -InstallRtk to install it through Cargo.'
    }
    elseif (-not (Test-Command 'cargo')) {
        throw 'Cargo is required for -InstallRtk. Install Rust/Cargo or install the official RTK Windows release manually.'
    }
    else {
        Invoke-SetupCommand 'Install RTK from rtk-ai/rtk' {
            cargo install --git https://github.com/rtk-ai/rtk
        }
    }
}

if (Test-Command 'rtk') {
    Invoke-SetupCommand 'Verify RTK' { rtk --version }

    if (-not $SkipHooks) {
        if (Test-Command 'codex') {
            Invoke-SetupCommand 'Configure the global RTK hook for Codex' {
                rtk init -g --codex
            }
        }
        else {
            Write-Output '[skip] Codex command not detected'
        }

        if (Test-Command 'claude') {
            Invoke-SetupCommand 'Configure the global RTK hook for Claude Code' {
                rtk init -g
            }
        }
        else {
            Write-Output '[skip] Claude Code command not detected'
        }

        if (Test-Command 'opencode') {
            Invoke-SetupCommand 'Configure the global RTK integration for OpenCode' {
                rtk init -g --opencode
            }
        }
        else {
            Write-Output '[skip] OpenCode command not detected'
        }

        Invoke-SetupCommand 'Configure the repository-local RTK integration for Antigravity' {
            Push-Location $repoRoot
            try {
                rtk init --agent antigravity
            }
            finally {
                Pop-Location
            }
        }
    }

    Invoke-SetupCommand 'Show the active RTK configuration' { rtk init --show }
}

if ($Plugins) {
    if (-not (Test-Path -LiteralPath $pluginManifest -PathType Leaf)) {
        throw "Plugin manifest not found: $pluginManifest"
    }
    if (-not (Test-Command 'codex')) {
        throw 'Codex is required when using -Plugins.'
    }

    Get-Content -LiteralPath $pluginManifest |
        ForEach-Object {
            $plugin = $_.Trim()
            if ($plugin -and -not $plugin.StartsWith('#')) {
                Invoke-SetupCommand "Install Codex plugin $plugin" {
                    codex plugin add $plugin
                }
            }
        }
}

$pythonCommand = if (Test-Command 'python3') {
    'python3'
}
elseif (Test-Command 'python') {
    'python'
}
elseif (Test-Command 'py') {
    'py'
}
else {
    $null
}

if ($pythonCommand) {
    foreach ($validationScript in @(
        'scripts/validate_agent_config.py',
        'scripts/validate_ai_efficiency.py'
    )) {
        Invoke-SetupCommand "Validate with $validationScript" {
            Push-Location $repoRoot
            try {
                & $pythonCommand $validationScript
            }
            finally {
                Pop-Location
            }
        }
    }
}
else {
    Write-Warning 'Python was not detected; agent configuration validation was skipped.'
}

Write-Output ''
Write-Output 'Setup complete.'
Write-Output 'Restart each detected coding agent or start a new conversation so hooks, rules, plugins, and Levyra skills are reloaded.'
Write-Output 'Use `rtk gain` and `rtk discover --all --since 7` to measure real command-output savings.'
