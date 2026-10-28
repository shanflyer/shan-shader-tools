# Changelog

## 0.2.0 — 2026-10-28

- Remove the CLion-specific module dependency for multi-IDE support.
- Use a shared 262.10968 minimum build across JetBrains 2026.2 products.
- Update Marketplace descriptions and IDE compatibility documentation.

## 0.1.2 — 2026-10-28

- Correct Marketplace Windows x64 platform declarations using OS/architecture dependencies.

## 0.1.1 — 2026-10-28

- Prepare the first Marketplace beta with a dedicated SVG logo and publisher metadata.
- Restrict supported IDEs to CLion, Windows x64, starting with build 262.10968.117.
- Locate the bundled server using the public IDE JAR-location API, removing internal IDE API usage.
- Upgrade Newtonsoft.Json to 13.0.4 to address the dependency audit finding.
- Preserve upstream HlslTools copyright and dependency notices.

## 0.1.0 — 2026-10-27

- Initial local preview with HlslTools semantic services and a bundled Windows x64 runtime.
