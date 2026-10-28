$ErrorActionPreference = 'Stop'
$pluginRoot = Split-Path -Parent $PSScriptRoot
$project = Join-Path $pluginRoot 'build/upstream/HlslTools-3cb3e76a8a5a35daf6f8e6fa5c32da7facf7175a/src/ShaderTools.LanguageServer/ShaderTools.LanguageServer.csproj'
$report = Join-Path $pluginRoot 'build/nuget-audit.json'
$output = & dotnet list $project package --vulnerable --include-transitive --format json
if ($LASTEXITCODE -ne 0) { throw 'NuGet vulnerability audit could not complete' }
$output | Set-Content -LiteralPath $report -Encoding utf8NoBOM
$audit = $output -join "`n" | ConvertFrom-Json
if ($audit.problems) { throw 'NuGet audit reported a data source problem' }
$vulnerable = @($audit.projects | ForEach-Object frameworks | ForEach-Object {
    @($_.topLevelPackages) + @($_.transitivePackages)
} | Where-Object { $_ -and $_.vulnerabilities })
if ($vulnerable.Count -gt 0) { throw "NuGet reported vulnerable dependencies: $($vulnerable.id -join ', ')" }
Write-Host 'PASS: no known vulnerable NuGet dependencies reported by the configured feeds'
