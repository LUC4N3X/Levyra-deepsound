param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$HarnessArgs
)

$root = Split-Path -Parent $PSScriptRoot
$script = Join-Path $root "scripts\agent_harness.py"
$argsToPass = $HarnessArgs

if ($HarnessArgs.Count -gt 0 -and $HarnessArgs[0] -eq "checkpoint") {
    $script = Join-Path $root "scripts\agent_checkpoint.py"
    $argsToPass = if ($HarnessArgs.Count -gt 1) { $HarnessArgs[1..($HarnessArgs.Count - 1)] } else { @() }
} elseif ($HarnessArgs.Count -gt 0 -and $HarnessArgs[0] -eq "router") {
    $script = Join-Path $root "scripts\agent_skill_router.py"
    $argsToPass = if ($HarnessArgs.Count -gt 1) { $HarnessArgs[1..($HarnessArgs.Count - 1)] } else { @() }
} elseif ($HarnessArgs.Count -gt 0 -and $HarnessArgs[0] -eq "audit") {
    $script = Join-Path $root "scripts\agent_stop_audit.py"
    $argsToPass = if ($HarnessArgs.Count -gt 1) { $HarnessArgs[1..($HarnessArgs.Count - 1)] } else { @() }
}

$python = Get-Command python -ErrorAction SilentlyContinue
if ($python) {
    & $python.Source $script @argsToPass
    exit $LASTEXITCODE
}

$python3 = Get-Command python3 -ErrorAction SilentlyContinue
if ($python3) {
    & $python3.Source $script @argsToPass
    exit $LASTEXITCODE
}

$py = Get-Command py -ErrorAction SilentlyContinue
if ($py) {
    & $py.Source -3 $script @argsToPass
    exit $LASTEXITCODE
}

exit 0
