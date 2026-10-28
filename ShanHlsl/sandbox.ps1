param(
    [string] $ClionHome = $env:CLION_HOME,
    [switch] $SmokeTest,
    [string] $ConfigDirectory = ""
)
$ErrorActionPreference = 'Stop'
if (-not $ClionHome) { $ClionHome = 'C:\Program Files\JetBrains\CLion 2026.2.3' }
$clionRoot = (Resolve-Path -LiteralPath $ClionHome).Path
$buildRoot = Join-Path $PSScriptRoot 'build'
$sandbox = Join-Path $buildRoot 'sandbox'
if (-not $ConfigDirectory) { $ConfigDirectory = Join-Path $sandbox 'config' }
$plugins = Join-Path $sandbox 'plugins'
$package = Join-Path $buildRoot 'package\ShanHlsl'
if (-not (Test-Path -LiteralPath $package)) { throw 'Run .\build.ps1 first' }
New-Item -ItemType Directory -Path $plugins -Force | Out-Null
Copy-Item -LiteralPath $package -Destination $plugins -Recurse -Force

if ($SmokeTest) {
    $smokeClasses = Join-Path $buildRoot 'smoke-classes'
    $smokeLib = Join-Path $plugins 'ShanHlslSmoke\lib'
    New-Item -ItemType Directory -Path $smokeClasses, $smokeLib -Force | Out-Null
    $sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\smoke\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
    & (Join-Path $clionRoot 'jbr\bin\javac.exe') --release 25 -encoding UTF-8 -classpath "$(Join-Path $buildRoot 'classes');$(Join-Path $clionRoot 'lib\*')" -d $smokeClasses @sources
    if ($LASTEXITCODE -ne 0) { throw 'Sandbox test compilation failed' }
    Copy-Item -Path (Join-Path $PSScriptRoot 'src\smoke\resources\*') -Destination $smokeClasses -Recurse -Force
    $jar = Join-Path $clionRoot 'jbr\bin\jar.exe'
    if (-not (Test-Path -LiteralPath $jar)) { $jar = (Get-Command jar.exe -ErrorAction Stop).Source }
    & $jar cf (Join-Path $smokeLib 'smoke.jar') -C $smokeClasses .
    if ($LASTEXITCODE -ne 0) { throw 'Sandbox test packaging failed' }
}

$product = Get-Content -LiteralPath (Join-Path $clionRoot 'product-info.json') -Raw | ConvertFrom-Json
$launch = $product.launch | Where-Object { $_.os -eq 'Windows' -and $_.arch -eq 'amd64' } | Select-Object -First 1
$vmOptions = @(Get-Content -LiteralPath (Join-Path $clionRoot $launch.vmOptionsFilePath) | Where-Object { $_ -and -not $_.StartsWith('#') })
$vmOptions += @($launch.additionalJvmArguments | ForEach-Object { $_.Replace('%IDE_HOME%', $clionRoot) })
$vmOptions += @(
    "-Didea.home.path=$clionRoot",
    "-Didea.config.path=$ConfigDirectory",
    "-Didea.system.path=$sandbox/system",
    "-Didea.plugins.path=$plugins",
    "-Didea.log.path=$sandbox/log",
    '-Didea.initially.ask.config=false',
    '-Didea.trust.all.projects=true',
    "-Dshan.hlsl.project=$PSScriptRoot",
    "-Dshan.hlsl.smoke.report=$sandbox/smoke-report.txt"
)
if ($SmokeTest) { $vmOptions += '-Djava.awt.headless=true' }
$classpath = ($launch.bootClassPathJarNames | ForEach-Object { Join-Path $clionRoot "lib/$_" }) -join ';'
$vmOptions += @('-classpath', $classpath, 'com.intellij.idea.Main')
if ($SmokeTest) { $vmOptions += 'shanHlslSmoke' }
else { $vmOptions += (Join-Path $PSScriptRoot 'examples') }
$argumentFile = Join-Path $sandbox 'launch.args'
$quoted = $vmOptions | ForEach-Object { '"' + $_.Replace('\', '/').Replace('"', '\"') + '"' }
Set-Content -LiteralPath $argumentFile -Value $quoted -Encoding utf8NoBOM
& (Join-Path $clionRoot $launch.javaExecutablePath) "@$argumentFile"
if ($LASTEXITCODE -ne 0) { throw "Sandbox exited with $LASTEXITCODE. See $sandbox/log/idea.log" }
