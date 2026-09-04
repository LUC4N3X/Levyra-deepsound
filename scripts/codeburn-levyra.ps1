[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $CodeBurnArgs
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectFilter = 'Levyra-deepsound'

if (-not (Get-Command npx -ErrorAction SilentlyContinue)) {
    throw 'CodeBurn requires Node.js 22.13+ with npm/npx.'
}

$commandArgs = if ($CodeBurnArgs -and $CodeBurnArgs.Count -gt 0) {
    $CodeBurnArgs
}
else {
    @('overview', '-p', 'week')
}

& npx -y "codeburn@0.9.24" @commandArgs --project $projectFilter
exit $LASTEXITCODE
