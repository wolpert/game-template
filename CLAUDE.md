# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project shape

libGDX game template, Java 21, Gradle (Kotlin DSL), multi-module:

- `core/` — shared `ApplicationListener` (`com.codeheadsystems.game.TheGame`) and game code consumed by every launcher.
- `lwjgl3/` — desktop launcher (`Lwjgl3Launcher`), distributable jars, and Construo native bundles.
- `android/` — `AndroidLauncher`, manifest, resources, and Box2D native packaging.
- `assets/` — runtime-loadable content; shared by both launchers (Android `assets.srcDirs` points at `../assets`).
- `art/` — source art (e.g. `.aseprite`); never read at runtime.

`org.gradle.daemon=false` in `gradle.properties` — every `./gradlew` invocation spins up a fresh JVM, so commands feel slower than typical Gradle projects. Don't "fix" this.

## Common commands

```bash
# Run desktop build
./gradlew lwjgl3:run

# Build/install Android (device or emulator attached)
./gradlew android:installDebug
./gradlew android:run                 # adb shell am start the installed app

# Full CI build (matches .github/workflows/ci.yml)
./gradlew build test --stacktrace

# Tests (JUnit 5 + Mockito; see core/src/test/...)
./gradlew :core:test
./gradlew :core:test --tests "*RenderSystem*"

# Desktop distributable jar (fat jar with all deps)
./gradlew lwjgl3:dist                 # alias for lwjgl3:jar
./gradlew lwjgl3:jarMac               # platform-trimmed variants
./gradlew lwjgl3:jarLinux
./gradlew lwjgl3:jarWin

# Native installers via Construo (jlink + per-OS bundles)
./gradlew lwjgl3:construo             # all targets: linuxX64, macM1, macX64, winX64
```

GraalVM native image is opt-in. Set `enableGraalNative=true` in `gradle.properties` to load the native-image plugin and `nativeimage.gradle.kts`; otherwise those code paths are dead.

## What you actually have vs. what the README promises

The README describes an aspirational template (Dagger DI, Ashley ECS, Box2D wiring, loading/menu/preferences screens, YAML config, Aseprite import pipeline, falling-blocks sample). Some of it is real now; most still isn't — Box2D wiring, screens, YAML, and the Aseprite pipeline aren't implemented, and several deps (gdx-ai, gdx-vfx, libgdx-utils-box2d, box2dlights) are pulled into `core/build.gradle.kts` but unused.

**Dagger is wired up.** The graph lives under `com.codeheadsystems.game.di`: `GameModule` provides the shared `SpriteBatch` / `Texture` / Ashley `Engine`, `GameComponent` exposes `inject(TheGame)`, and `TheGame.create()` builds the component (`DaggerGameComponent.create().inject(this)`) so providers run after libGDX has initialized GL. Annotation processing is configured via `annotationProcessor "com.google.dagger:dagger-compiler:$daggerVersion"` in `core/build.gradle.kts`; generated sources land at `core/build/generated/sources/annotationProcessor/...`.

**Ashley is wired through Dagger.** Components live under `com.codeheadsystems.game.ecs.component` (pure-data, default constructors): `PositionComponent` (x, y, plus an int `z` layer), `TextureComponent` (a single `TextureRegion` so atlases and stand-alone textures share one render path), and `AnimationComponent` (a libGDX `Animation<TextureRegion>` plus elapsed time). Systems live under `com.codeheadsystems.game.ecs.system`, are `@Singleton` with `@Inject` constructors, and are registered onto the `PooledEngine` inside `GameModule.provideEngine(...)` — adding a system means adding a constructor parameter there and calling `engine.addSystem(...)`. `AnimationSystem` (priority 0) advances elapsed time and writes the current frame back into `TextureComponent.region`; `RenderSystem` is a `SortedIteratingSystem` (priority 10) that sorts by `PositionComponent.z` ascending so lower-z entities draw first (background-to-foreground). `TheGame.render()` is a one-liner (`engine.update(delta)`); per-frame work belongs in a system.

**Aseprite import pipeline is real.** `./gradlew buildAtlases` runs `exportAseprite` (Aseprite CLI with `--split-tags` → `build/aseprite-frames/<basename>_<tag>_<tagframe>.png`) followed by `packTextures` (libGDX TexturePacker → `assets/atlases/<appName>.atlas` + packed PNG). The `aseprite.bin` path comes from `local.properties` (leading `~` is expanded). It must point at the **executable itself**, not a wrapper script — wrappers that `exec` another process can lose env and silently produce zero files. The pipeline is **not** hooked into the normal build; contributors run it manually after editing `.aseprite` source and commit the regenerated atlas, so CI doesn't need Aseprite. The trailing `_<index>` filenames intentionally line up with TexturePacker's region-index parsing, so `atlas.findRegions("<basename>_<tag>")` returns frames in order, ready for `Animation`.

**YAML config is wired through Dagger.** SnakeYAML loads `assets/config/game.yaml` into `GameConfig` via `ConfigLoader`, which is configured with `BeanAccess.FIELD` so POJOs are plain public fields (no getters/setters). `GameModule.provideGameConfig` opens the asset with `Gdx.files.internal(...).reader()` and delegates. Adding a new config type means: POJO under `config/`, a `@Provides @Singleton` method that opens its asset, and a `ConfigLoaderTest`-style unit test against a `StringReader`. **Android caveat:** SnakeYAML references `java.beans.*` (absent on Android) — `android/proguard-rules.pro` carries `-dontwarn java.beans.**` and a `-keep class com.codeheadsystems.game.config.**` rule so R8 doesn't strip reflectively populated fields. Move/rename the config package and you must update that keep rule.

**Tests use JUnit 5 + Mockito.** `core/src/test/java/.../RenderSystemTest` is the pattern: build a `PooledEngine`, register the system with a mocked `SpriteBatch`, add entities, call `engine.update(delta)`, and verify with Mockito `InOrder`. This exercises family matching and component mappers in addition to the system code, so prefer it over invoking system methods directly. Mockito is loaded as an explicit `-javaagent` via the `mockitoAgent` configuration to avoid the deprecated self-attach path on JDK 21+ — reuse that configuration when adding mocking deps.

## Asset list generation

`generateAssetList` (root `build.gradle.kts`) is wired into every non-Android subproject's `processResources`. It recursively walks `assets/` and rewrites `assets/assets.txt`. The runtime relies on this manifest, so **don't hand-edit `assets/assets.txt`** — re-run the build (or `:core:processResources`) after adding/removing asset files.

## Android natives

`android:copyAndroidNatives` extracts `.so` files from the `natives` configuration into `android/libs/<abi>/` and is hooked onto every `merge*JniLibFolders` task. If you bump `gdxVersion`, ABI-specific natives need to be added to the `natives` dependencies in `android/build.gradle.kts` for each new platform.

## Renaming the project

See the README's "Using this template" section for the canonical checklist. The knobs it points at:

- `extra["appName"]` in root `build.gradle.kts` — drives jar names and Construo bundle naming.
- `eclipse.project.name = "game-template-parent"` in root `build.gradle.kts`; module includes in `settings.gradle.kts`.
- `namespace` / `applicationId` in `android/build.gradle.kts`; activity reference in the `android:run` task; `AndroidManifest.xml`.
- `application.mainClass` in `lwjgl3/build.gradle.kts` and Construo `identifier` for macOS.
- Java package paths under each module's `src/main/java/com/codeheadsystems/game/...`.
