param(
    [string] $ClionHome = 'C:\Program Files\JetBrains\CLion 2026.2.3',
    [string] $Version = '0.1.1',
    [string] $PluginRoot = (Split-Path -Parent $PSScriptRoot)
)
$ErrorActionPreference = 'Stop'
$work = Join-Path $PSScriptRoot 'build'
$downloads = Join-Path $work 'downloads'
New-Item -ItemType Directory -Path $downloads -Force | Out-Null
$verifier = Join-Path $downloads 'verifier-cli-1.411-all.jar'
if (-not (Test-Path -LiteralPath $verifier)) {
    Invoke-WebRequest -Uri 'https://github.com/JetBrains/intellij-plugin-verifier/releases/download/1.411/verifier-cli-1.411-all.jar' -OutFile $verifier
}
if ((Get-FileHash -LiteralPath $verifier -Algorithm SHA256).Hash -ne 'D6CC1C1F17761BB5EFFF187F98B63FB792C7EF2A2FB9202BB5DBEA05EBC02BFA') {
    throw 'Plugin Verifier SHA256 mismatch'
}
$java = Join-Path $ClionHome 'jbr/bin/java.exe'
foreach ($name in @('ShanGlsl', 'ShanHlsl')) {
    $archive = Join-Path $PluginRoot "$name/build/distributions/$name-$Version.zip"
    if (-not (Test-Path -LiteralPath $archive)) { throw "Missing plugin archive: $archive" }
    $reports = Join-Path $work "verifier-final/$name"
    $verifierHome = Join-Path $work "verifier-home-$name"
    & $java "-Dplugin.verifier.home.dir=$verifierHome" -Xmx2g -jar $verifier check-plugin `
        -verification-reports-dir $reports $archive $ClionHome
    if ($LASTEXITCODE -ne 0) { throw "Plugin Verifier failed for $name" }
    $verdicts = @(Get-ChildItem -LiteralPath $reports -Filter verification-verdict.txt -Recurse)
    if ($verdicts.Count -eq 0) { throw "No verification verdict for $name" }
    foreach ($verdict in $verdicts) {
        if ((Get-Content -LiteralPath $verdict.FullName -Raw).Trim() -ne 'Compatible') {
            throw "Verification requires attention: $($verdict.FullName)"
        }
    }
}
Write-Host 'PASS: both plugins are binary-compatible without internal/deprecated API findings'
