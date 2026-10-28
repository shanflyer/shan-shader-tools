param([switch] $Publish)
$ErrorActionPreference = 'Stop'
$pluginRoot = Split-Path -Parent $PSScriptRoot
$buildRoot = Join-Path $pluginRoot 'build'
$commit = '3cb3e76a8a5a35daf6f8e6fa5c32da7facf7175a'
$sha256 = 'C7BA38646847E26C5468F85B4EF86B889D50661F4B66B9D8F24C3403787E8DB5'
$downloads = Join-Path $buildRoot 'downloads'
$archive = Join-Path $downloads 'hlsltools-upstream.zip'
New-Item -ItemType Directory -Path $downloads -Force | Out-Null
if (-not (Test-Path -LiteralPath $archive)) {
    Invoke-WebRequest -Uri "https://codeload.github.com/tgjones/HlslTools/zip/$commit" -OutFile $archive
}
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $sha256) {
    throw 'HlslTools source archive checksum mismatch'
}
$upstream = Join-Path $buildRoot "upstream\HlslTools-$commit"
if (-not (Test-Path -LiteralPath $upstream)) {
    Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $buildRoot 'upstream')
}
$serverDirectory = Join-Path $upstream 'src\ShaderTools.LanguageServer'
$project = Join-Path $serverDirectory 'ShaderTools.LanguageServer.csproj'
$text = Get-Content -LiteralPath $project -Raw
$text = $text.Replace('<TargetFramework>net6.0</TargetFramework>', '<TargetFramework>net10.0</TargetFramework>')
$text = $text.Replace('<PublishTrimmed>true</PublishTrimmed>', '<PublishTrimmed>false</PublishTrimmed>')
$text = $text.Replace('<PublishSingleFile>true</PublishSingleFile>', '<PublishSingleFile>false</PublishSingleFile>')
if (-not $text.Contains('Include="Newtonsoft.Json"')) {
    $text = $text.Replace('</Project>', '  <ItemGroup><PackageReference Include="Newtonsoft.Json" Version="13.0.4" /></ItemGroup>' + "`n</Project>")
}
Set-Content -LiteralPath $project -Value $text -Encoding utf8NoBOM

# Keep all local extensions separate from the pinned upstream source.
$patches = Join-Path $PSScriptRoot 'extensions'
if (Test-Path -LiteralPath $patches) {
    Copy-Item -Path (Join-Path $patches '*.cs') -Destination $serverDirectory -Force
}
$hostFile = Join-Path $serverDirectory 'LanguageServerHost.cs'
$hostText = Get-Content -LiteralPath $hostFile -Raw
if (-not $hostText.Contains('WithShanHandlers')) {
    $hostText = $hostText.Replace('.AddHandler(new SignatureHelpHandler(_workspace, documentSelector)))',
        '.AddHandler(new SignatureHelpHandler(_workspace, documentSelector)).WithShanHandlers(_workspace, documentSelector))')
    Set-Content -LiteralPath $hostFile -Value $hostText -Encoding utf8NoBOM
}

$completionFile = Join-Path $serverDirectory 'Handlers\CompletionHandler.cs'
$completionText = Get-Content -LiteralPath $completionFile -Raw
$completionText = $completionText.Replace('request.Context.TriggerKind == OmniSharp.Extensions.LanguageServer.Protocol.Models.CompletionTriggerKind.TriggerCharacter)',
    'request.Context?.TriggerKind == OmniSharp.Extensions.LanguageServer.Protocol.Models.CompletionTriggerKind.TriggerCharacter && !string.IsNullOrEmpty(request.Context.TriggerCharacter))')
Set-Content -LiteralPath $completionFile -Value $completionText -Encoding utf8NoBOM

$signatureFile = Join-Path $serverDirectory 'Handlers\SignatureHelpHandler.cs'
$signatureText = Get-Content -LiteralPath $signatureFile -Raw
if (-not $signatureText.Contains('TriggerCharacters')) {
    $signatureText = $signatureText.Replace('DocumentSelector = documentSelector',
        'DocumentSelector = documentSelector, TriggerCharacters = new Container<string>("(", ",")')
    Set-Content -LiteralPath $signatureFile -Value $signatureText -Encoding utf8NoBOM
}

if ($Publish) {
    & dotnet publish $project --configuration Release --runtime win-x64 --self-contained true `
        -p:PublishTrimmed=false -p:PublishSingleFile=false `
        --output (Join-Path $buildRoot 'server\win-x64') --nologo
    if ($LASTEXITCODE -ne 0) { throw 'HLSL language server publish failed' }
    & python (Join-Path $PSScriptRoot 'collect_licenses.py')
    if ($LASTEXITCODE -ne 0) { throw 'Dependency notice collection failed' }
    & (Join-Path $PSScriptRoot 'audit_dependencies.ps1')
}
Write-Host "Prepared HlslTools $commit for .NET 10 (untrimmed MEF composition)"
