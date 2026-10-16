param(
    [Parameter(Mandatory=$true)][int]$StudyProcessId,
    [Parameter(Mandatory=$true)][string]$Dataset,
    [Parameter(Mandatory=$true)][string]$Output,
    [string]$Maven='C:\Users\thomas\.maven\maven-3.10.0\bin\mvn.cmd'
)
$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
$workspace=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $workspace
$datasetPath=(Resolve-Path -LiteralPath $Dataset).Path
$statusPath=Join-Path $datasetPath 'analysis-job.json'
$status=@{status='waiting';study_pid=$StudyProcessId;analysis_pid=$PID;started_utc=[DateTime]::UtcNow.ToString('o')}
function Publish { $status | ConvertTo-Json | Set-Content -LiteralPath $statusPath -Encoding UTF8 }
Publish
try {
    $study=Get-Process -Id $StudyProcessId -ErrorAction SilentlyContinue
    if ($null -ne $study) { Wait-Process -InputObject $study }
    $results=Import-Csv -LiteralPath (Join-Path $datasetPath 'results.csv')
    if ($results.Count -ne 144) { throw "Comparison incomplete: $($results.Count)/144 trials" }
    $status.status='probing';Publish
    & $Maven -B -ntp test '-Dtest=FunctionSignalControlsTest,FunctionSignalStudyTest' `
        "-Dfunction.signals.folder=$datasetPath" "-Dfunction.signals.output=$(Join-Path $datasetPath 'signals.csv')" `
        '-DargLine=-Xmx2g -Djava.util.concurrent.ForkJoinPool.common.parallelism=4'
    if ($LASTEXITCODE -ne 0) { throw "Signal probes failed: exit $LASTEXITCODE" }
    $status.status='analyzing';Publish
    & python (Join-Path $PSScriptRoot 'finish_function_comparison.py') $datasetPath --output $Output
    if ($LASTEXITCODE -ne 0) { throw "Result analysis failed: exit $LASTEXITCODE" }
    $status.status='completed';$status.finished_utc=[DateTime]::UtcNow.ToString('o');Publish
    Copy-Item -LiteralPath $statusPath -Destination (Join-Path $Output 'analysis-job.json')
} catch {
    $status.status='failed';$status.error=$_.Exception.Message;$status.finished_utc=[DateTime]::UtcNow.ToString('o');Publish
    Write-Error $_ -ErrorAction Continue
    exit 1
}
