[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $CodeBurnArgs
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectFilter = 'Levyra-deepsound'

if (-not (Get-Command npx -ErrorAction SilentlyContinue)) {
    throw 'CodeBurn requires Node.js/npm with npx.'
}

$commandArgs = if ($CodeBurnArgs -and $CodeBurnArgs.Count -gt 0) {
    $CodeBurnArgs
}
else {
    @('report', '-p', 'week')
}

& npx -y "codeburn@0.9.20" @commandArgs --project $projectFilter
exit $LASTEXITCODE
