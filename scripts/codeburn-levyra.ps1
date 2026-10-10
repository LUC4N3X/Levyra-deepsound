Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectFilter = 'Levyra-deepsound'
$projectAwareCommands = @(
    'report',
    'today',
    'month',
    'overview',
    'status',
    'export',
    'web'
)

if (-not (Get-Command npx -ErrorAction SilentlyContinue)) {
    throw 'CodeBurn requires Node.js 22.13+ with npm/npx.'
}

$commandArgs = @($args)
if ($commandArgs.Count -eq 0) {
    $commandArgs = @('overview', '-p', 'week')
}

$subcommand = [string] $commandArgs[0]
if ($projectAwareCommands -contains $subcommand) {
    & npx -y "codeburn@0.9.24" @commandArgs --project $projectFilter
}
else {
    & npx -y "codeburn@0.9.24" @commandArgs
}

exit $LASTEXITCODE
