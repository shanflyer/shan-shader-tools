# Compatibility — 0.2.0

Both plugins target IntelliJ Platform builds `262.10968` through `262.*` (the 2026.2 line).
Neither plugin requires CLion or its C/C++ modules.

| Plugin | Required shared modules | Native runtime |
| --- | --- | --- |
| Shan GLSL | Platform, language | User-provided glslang; the plugin is platform-independent |
| Shan HLSL | Platform, language, native LSP | Bundled Windows x64 HlslTools server and .NET runtime |

Marketplace currently recognizes HLSL support for CLion, IntelliJ IDEA, PyCharm, WebStorm,
PhpStorm, Rider, GoLand, RubyMine, DataGrip and RustRover releases in this build range.
The GLSL plugin also supports products with only the shared language APIs.
HLSL excludes Android Studio and legacy open-source IDEA Community builds because they lack
the native LSP module. DataSpell currently has no release in this build range.
These product declarations are not a claim that every individual product was manually exercised.

## Binary verification

JetBrains Plugin Verifier 1.411 reports the following results for the packaged 0.2.0 archives:

| IDE distribution | Shan GLSL | Shan HLSL |
| --- | --- | --- |
| CLion 2026.2.3 / 262.10968.117 | Compatible | Compatible |
| WebStorm 2026.2.3 / 262.10968.77 | Compatible | Compatible |

The checks include API compatibility, internal API usage and deprecated API usage.
HLSL uses the documented `-ignore-os-arch` verifier option for synthetic OS/architecture modules;
no API findings are muted. The Marketplace archive still requires Windows x64.
The WebStorm SDK was the official Linux distribution; these checks validate Java APIs,
not execution of the Windows language server on Linux.

Both archives contain the same Java class bytes as the prior CLion release: this update changes
compatibility metadata, version numbers and documentation, preserving language behavior.
The earlier release was also exercised in a full CLion UI with the real HLSL server.

## Reproduce

Build both plugins, then run `release/verify.ps1`. Pass `-IdeHomes` to verify additional local
IDE distributions; `-ClionHome` selects the JBR used to execute the verifier and read runtime APIs.
See the [official LSP support list](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html)
and [OS/architecture declarations](https://plugins.jetbrains.com/docs/marketplace/os-architecture-specific-plugin-versions.html).

## Release checksums

- `ShanGlsl-0.2.0.zip`: `41d7d96d3b25a814c0bb06c0cc786be28e7ba8a6d767ba28537187e973606629`
- `ShanHlsl-0.2.0.zip`: `01bb536c9aef10160d44db5346748f207f186b7633b2c5a4f2f8fb76df6e2f78`
