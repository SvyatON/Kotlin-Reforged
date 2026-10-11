Kotlin Reforged modifications by SvyatON, 2026-10-11.

<img src="src/kffmod/common/resources/kotlinforforge_icon.png" alt="Kotlin Reforged" width="96" align="right">

# Kotlin Reforged

Kotlin language support and utilities for **Minecraft 1.21.6-26.3 on Forge**.

An unofficial fork of [Kotlin for Forge](https://github.com/thedarkcolour/KotlinForForge). Original author: **thedarkcolour**. Forge port: **SvyatON**.

- Loads Kotlin classes and `object` declarations annotated with `@Mod`.
- Registers Kotlin object, static, file and multifile event subscribers through Forge EventBus 7.
- Provides registry delegates, capability utilities, vector extensions and other Forge helpers.
- Bundles Kotlin 2.4.0, coroutines 1.11.0, serialization 1.11.0 (JSON and CBOR), atomicfu 0.33.0, datetime 0.8.0 and kotlinx-io 0.9.1.

One installation JAR covers the supported versions. Place it in your instance's `mods` directory.

The mod ID is `kotlin_reforged`. The language provider remains `kotlinforforge`, and public packages retain `thedarkcolour.kotlinforforge`. Dependent mods must use the new mod ID and support EventBus 7.

## Building

Install JDK 25 to run Gradle, a JDK 21 compilation toolchain, and Python 3.11 or newer. Run from the repository root:

```powershell
.\gradlew.bat buildForge
python package-forge.py
```

On Linux or macOS, use `./gradlew buildForge`. The installation JAR is written to `Готовые моды/`. Build outputs and local tools are excluded from Git.

To generate the local Maven repository in `build/maven`, run `./gradlew publish` after packaging. The coordinates are `thedarkcolour:kotlinforforge:7.0.0+1.21.6-26.3-reforged`.

See [changelog](changelog.md) and [LICENSE](LICENSE). Bundled library notices and licenses are included in the mod resources.

## License and release sources

Kotlin Reforged adapter code, including the fork modifications, remains under GNU LGPL version 2.1; see [LICENSE](LICENSE). Bundled dependencies retain their own licenses; see the [library notices](src/kffmod/common/resources/META-INF/THIRD-PARTY-LIBRARIES.md).

Packaging writes `7.0.0+1.21.6-26.3-sources.jar` next to the installation JAR in `Готовые моды/`. It contains the full adapter sources, resources, licenses and build scripts. Attach the matching `sources.jar` as an additional source file to every Modrinth release, keep the installation JAR as the primary file, and preserve the source revision with a version tag or unchanged version branch. The sources are also available as a ZIP under `build/releases/`. Generated archives are excluded from Git.
