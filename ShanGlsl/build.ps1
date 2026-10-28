param(
    [string] $ClionHome = $env:CLION_HOME,
    [switch] $SkipTests
)

$ErrorActionPreference = 'Stop'
if (-not $ClionHome) { $ClionHome = 'C:\Program Files\JetBrains\CLion 2026.2.3' }
$clionRoot = (Resolve-Path -LiteralPath $ClionHome).Path
$projectRoot = $PSScriptRoot
$buildRoot = Join-Path $projectRoot 'build'
$classes = Join-Path $buildRoot 'classes'
$testClasses = Join-Path $buildRoot 'test-classes'
$java = Join-Path $clionRoot 'jbr\bin\java.exe'
$javac = Join-Path $clionRoot 'jbr\bin\javac.exe'
$jar = Join-Path $clionRoot 'jbr\bin\jar.exe'
# JetBrains Runtime may omit jar.exe; use the JDK jar utility for packaging only.
if (-not (Test-Path -LiteralPath $jar)) {
    $jarCommand = Get-Command jar.exe -ErrorAction Stop
    $jar = $jarCommand.Source
}
if (-not (Test-Path -LiteralPath $javac)) { throw "A JDK 25 javac is required: $javac" }
$product = Get-Content -LiteralPath (Join-Path $clionRoot 'product-info.json') -Raw | ConvertFrom-Json
if ($product.buildNumber -notlike '262.*') { throw "This release targets CLion 262.*; found $($product.buildNumber)" }

# Verify every recursive deletion stays inside this plugin's build directory.
foreach ($target in @($classes, $testClasses, (Join-Path $buildRoot 'package'))) {
    $resolvedTarget = [IO.Path]::GetFullPath($target)
    if (-not $resolvedTarget.StartsWith([IO.Path]::GetFullPath($buildRoot) + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase)) { throw "Unsafe build target: $resolvedTarget" }
    if (Test-Path -LiteralPath $resolvedTarget) { Remove-Item -LiteralPath $resolvedTarget -Recurse -Force }
}
New-Item -ItemType Directory -Path $classes, $testClasses -Force | Out-Null
$libraryPath = Join-Path $clionRoot 'lib\*'
$sourceRoot = Join-Path $projectRoot 'src\main\java'
$sources = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' | ForEach-Object FullName)
& $javac --release 25 -encoding UTF-8 -classpath $libraryPath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Plugin compilation failed' }
Copy-Item -Path (Join-Path $projectRoot 'src\main\resources\*') -Destination $classes -Recurse -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'LICENSE'), (Join-Path $projectRoot 'THIRD-PARTY-NOTICES.txt') -Destination (Join-Path $classes 'META-INF') -Force

if (-not $SkipTests) {
    $tests = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\test\java') -Recurse -Filter '*.java' | ForEach-Object FullName)
    & $javac --release 25 -encoding UTF-8 -classpath "$classes;$libraryPath" -d $testClasses @tests
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
    & $java -ea '-Djava.awt.headless=true' -classpath "$testClasses;$classes;$libraryPath" dev.shanflyer.glsl.CoreTests
    if ($LASTEXITCODE -ne 0) { throw 'Core tests failed' }
}
$package = Join-Path $buildRoot 'package\ShanGlsl'
$lib = Join-Path $package 'lib'
$dist = Join-Path $buildRoot 'distributions'
New-Item -ItemType Directory -Path $lib, $dist -Force | Out-Null
& $jar cf (Join-Path $lib 'shan-glsl-0.2.0.jar') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed' }
$zip = Join-Path $dist 'ShanGlsl-0.2.0.zip'
Compress-Archive -LiteralPath $package -DestinationPath $zip -Force
Write-Host "Built $zip for CLion $($product.version) ($($product.buildNumber))"
