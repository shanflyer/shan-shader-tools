# Shan Shader Tools

Two independent shader editing plugins for CLion: **Shan GLSL** and **Shan HLSL**.

| Plugin | Features | Runtime |
| --- | --- | --- |
| [Shan GLSL](ShanGlsl/README.md) | Syntax colors, completion, folding, local compiler diagnostics, includes | User-installed glslang |
| [Shan HLSL](ShanHlsl/README.md) | HlslTools semantic completion, signature help, definitions, hover, diagnostics and formatting | Bundled Windows x64 server |

## Compatibility

The 0.1.1 beta targets CLion 2026.2 build 262.10968.117 and later 262.* builds.
This release is tested on Windows x64. HLSL requires Windows x64.
The two plugins can be installed together. Disable other plugins claiming the same file extensions if associations conflict.

## Build

Use PowerShell 7, the JDK bundled with CLion, Python 3, and .NET 10 SDK for the HLSL server.

```powershell
cd ShanGlsl
./build.ps1
cd ../ShanHlsl
./build.ps1
```

Build scripts support `-ClionHome` for another installation directory.
The HLSL script downloads a fixed upstream source archive, verifies its SHA256, and applies the documented local patches.
Build output, IDE caches and credentials are excluded from this repository.

## Upstream and licenses

- GLSL implementation: [MIT](ShanGlsl/LICENSE). The vocabulary originates from Daniel Scherzer's GLSLhelper; see [notices](ShanGlsl/THIRD-PARTY-NOTICES.txt).
- HLSL implementation and modified HlslTools server: [Apache 2.0 and retained upstream notices](ShanHlsl/LICENSE); see [notices](ShanHlsl/THIRD-PARTY-NOTICES.txt).
- Each plugin has its own privacy document, changelog and detailed README.

This project is not affiliated with JetBrains, Microsoft, Daniel Scherzer or Tim Jones.
Report reproducible problems through this repository's Issues page.
