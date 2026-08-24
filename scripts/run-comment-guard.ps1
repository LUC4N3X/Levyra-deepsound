$root = Split-Path -Parent $PSScriptRoot
$script = Join-Path $root "scripts\comment_guard_hook.py"

$python = Get-Command python -ErrorAction SilentlyContinue
if ($python) {
    & $python.Source $script
    exit $LASTEXITCODE
}

$python3 = Get-Command python3 -ErrorAction SilentlyContinue
if ($python3) {
    & $python3.Source $script
    exit $LASTEXITCODE
}

$py = Get-Command py -ErrorAction SilentlyContinue
if ($py) {
    & $py.Source -3 $script
    exit $LASTEXITCODE
}

exit 0
