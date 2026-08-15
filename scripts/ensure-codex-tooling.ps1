[CmdletBinding()]
param(
    [switch] $DryRun,
    [switch] $Quiet,
    [switch] $SkipIndex
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$mattSkillSource = 'mattpocock/skills'
$mattSkills = @(
    'setup-matt-pocock-skills',
    'grill-with-docs',
    'wayfinder',
    'to-spec',
    'to-tickets',
    'implement',
    'tdd',
    'diagnosing-bugs',
    'code-review',
    'domain-modeling'
)
$failures = New-Object System.Collections.Generic.List[string]

function Test-Command {
    param([Parameter(Mandatory)][string] $Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Write-Step {
    param([Parameter(Mandatory)][string] $Message)
    if (-not $Quiet) {
        Write-Output $Message
    }
}

function Invoke-FailOpen {
    param(
        [Parameter(Mandatory)][string] $Label,
        [Parameter(Mandatory)][scriptblock] $Command
    )
    if ($DryRun) {
        Write-Step "[dry-run] $Label"
        return
    }
    try {
        $global:LASTEXITCODE = 0
        & $Command
        if ($LASTEXITCODE -ne 0) {
            throw "exit code $LASTEXITCODE"
        }
    }
    catch {
        $failures.Add("$Label`: $($_.Exception.Message)")
    }
}

function Get-PythonCommand {
    foreach ($candidate in @('python3', 'python', 'py')) {
        if (Test-Command $candidate) {
            return $candidate
        }
    }
    return $null
}

$ensureRtk = Join-Path $PSScriptRoot 'ensure-rtk.ps1'
if (Test-Path -LiteralPath $ensureRtk -PathType Leaf) {
    Invoke-FailOpen 'Ensure pinned RTK' {
        & $ensureRtk -Quiet
    }
}
else {
    $failures.Add('Ensure pinned RTK: scripts/ensure-rtk.ps1 is missing')
}

$pythonCommand = Get-PythonCommand
$jCodeMunchRuntime = Join-Path $PSScriptRoot 'codex_jcodemunch.py'
if (-not $pythonCommand) {
    $failures.Add('Ensure jCodeMunch: Python is unavailable')
}
elseif (-not (Test-Path -LiteralPath $jCodeMunchRuntime -PathType Leaf)) {
    $failures.Add('Ensure jCodeMunch: scripts/codex_jcodemunch.py is missing')
}
else {
    Invoke-FailOpen 'Ensure pinned jCodeMunch' {
        & $pythonCommand $jCodeMunchRuntime ensure --quiet
    }
    if (-not $SkipIndex) {
        Invoke-FailOpen 'Refresh Levyra jCodeMunch index' {
            & $pythonCommand $jCodeMunchRuntime index --quiet
        }
    }
}

$skillRoot = Join-Path $HOME '.agents\skills'
$missingMattSkills = @(
    $mattSkills | Where-Object {
        -not (Test-Path -LiteralPath (Join-Path $skillRoot "$_\SKILL.md") -PathType Leaf)
    }
)
if ($missingMattSkills.Count -gt 0) {
    if (-not (Test-Command 'npx')) {
        $failures.Add('Ensure Matt Pocock skills: npx is unavailable')
    }
    else {
        Invoke-FailOpen 'Install missing Matt Pocock Codex skills' {
            $skillArgs = @('skills@latest', 'add', $mattSkillSource, '-g', '-a', 'codex', '-y')
            foreach ($skill in $mattSkills) {
                $skillArgs += @('-s', $skill)
            }
            & npx @skillArgs *> $null
        }
    }
}

if ($failures.Count -gt 0) {
    if (-not $Quiet) {
        foreach ($failure in $failures) {
            Write-Warning $failure
        }
    }
    exit 1
}

Write-Step '[ok] Levyra Codex tooling is ready.'
exit 0
