param(
    [Parameter(Mandatory=$true)][string]$Source,
    [Parameter(Mandatory=$true)][string]$Output,
    [string]$Maven = 'C:\Users\thomas\.maven\maven-3.10.0\bin\mvn.cmd',
    [int]$Steps = 600000,
    [int]$Seeds = 3,
    [int]$CheckpointInterval = 60000
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$sourcePath = (Resolve-Path -LiteralPath $Source).Path
$mavenPath = (Resolve-Path -LiteralPath $Maven).Path
$outputPath = [IO.Path]::GetFullPath($Output)
if (Test-Path -LiteralPath $outputPath) { throw 'Use a new output directory to preserve earlier results.' }
if ($Steps -le 0 -or $Seeds -le 0 -or $CheckpointInterval -le 0) { throw 'Budgets must be positive.' }
New-Item -ItemType Directory -Path $outputPath | Out-Null
$sourceCopy = Join-Path $outputPath 'source.cells'
Copy-Item -LiteralPath $sourcePath -Destination $sourceCopy
[ordered]@{
    commit = (& git -C $workspace rev-parse HEAD)
    original_source = $sourcePath
    source_copy = $sourceCopy
    sha256 = (Get-FileHash -LiteralPath $sourceCopy -Algorithm SHA256).Hash
    steps = $Steps
    seeds = $Seeds
    planned_trials = 3 * $Seeds
    checkpoint_interval = $CheckpointInterval
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputPath 'launch.json') -Encoding UTF8
function Quote-PowerShell([string]$value) { "'" + $value.Replace("'", "''") + "'" }
$arguments = @('-B', '-ntp', 'test', '-Dtest=NoneEnergyStudyTest',
    "-Dnone.energy.file=$sourceCopy", "-Dnone.energy.output=$outputPath",
    '-Dnone.energy.founders_per_section=1', '-Dnone.energy.floor=true',
    '-Dnone.energy.width=600', '-Dnone.energy.height=400',
    "-Dnone.energy.steps=$Steps", "-Dnone.energy.seeds=$Seeds", '-Dnone.energy.interval=600',
    "-Dnone.energy.checkpoint_interval=$CheckpointInterval",
    '-DargLine=-Xmx2g -Djava.util.concurrent.ForkJoinPool.common.parallelism=4')
$quotedArgs = ($arguments | ForEach-Object { Quote-PowerShell $_ }) -join ', '
$command = @"
`$ErrorActionPreference = 'Stop'
`$ProgressPreference = 'SilentlyContinue'
Set-Location -LiteralPath $(Quote-PowerShell $workspace)
`$statusPath = $(Quote-PowerShell (Join-Path $outputPath 'job.json'))
`$status = @{ status='running'; pid=`$PID; started_utc=[DateTime]::UtcNow.ToString('o') }
`$status | ConvertTo-Json | Set-Content -LiteralPath `$statusPath -Encoding UTF8
try {
    `$mavenArgs = @($quotedArgs)
    & $(Quote-PowerShell $mavenPath) @mavenArgs
    `$code = `$LASTEXITCODE
    `$status.exit_code = `$code
    `$status.status = if (`$code -eq 0) { 'completed' } else { 'failed' }
} catch {
    `$status.status='failed'; `$status.error=`$_.Exception.Message
    Write-Error `$_ -ErrorAction Continue
}
`$status.finished_utc=[DateTime]::UtcNow.ToString('o')
`$status | ConvertTo-Json | Set-Content -LiteralPath `$statusPath -Encoding UTF8
"@
$encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
$job = Start-Process -FilePath 'powershell.exe' -ArgumentList @('-NoProfile', '-OutputFormat', 'Text', '-EncodedCommand', $encoded) `
    -WorkingDirectory $workspace -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $outputPath 'run.log') `
    -RedirectStandardError (Join-Path $outputPath 'error.log')
Write-Output "Started PID $($job.Id): $outputPath"
