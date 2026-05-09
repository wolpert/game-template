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

The README describes the template's features (Dagger DI, Ashley ECS, Box2D wiring, loading/menu/preferences screens, YAML config, Aseprite import pipeline, falling-blocks sample) — all are implemented and wired through the Dagger graph described below.

**Dagger is wired up, split scaffold/sample.** The graph is two abstract `@Module`s: `core/.../di/CoreModule.java` (scaffold-only — `SpriteBatch`, `AssetManager`, `Skin`, `Preferences`, `World`, `GameConfig`, the scaffold ECS systems via `@IntoSet EntitySystem`, the scaffold `LoadableAsset` set, and the `@BindsOptionalOf @Sample` slots) and `core/.../sample/SampleModule.java` (the dodge demo — `TextureAtlas` provider, the demo's two `@IntoSet EntitySystem`s, the `SampleAsset` set, and the `@Sample`-qualified bindings that fill the optional slots). `GameComponent.modules = {CoreModule.class, SampleModule.class}`; deleting `core/.../sample/` and dropping `SampleModule.class` from that array leaves a clean scaffold (the `@BindsOptionalOf` slots resolve to empty). `TheGame.create()` builds the component (`DaggerGameComponent.create().inject(this)`) so providers run after libGDX has initialized GL. Annotation processing is configured via `annotationProcessor "com.google.dagger:dagger-compiler:$daggerVersion"` in `core/build.gradle.kts`; generated sources land at `core/build/generated/sources/annotationProcessor/...`. The `@Sample` qualifier (`core/.../di/Sample.java`) plus `@BindsOptionalOf @Sample X` is the seam that lets demo code rebind scaffold defaults (`InputGate`, debug-overlay extras, the sample game `Screen`) without scaffold importing anything from `sample/`.

**Ashley is wired through Dagger multibindings.** Components (`com.codeheadsystems.game.ecs.component`) are pure-data with default constructors: `PositionComponent` (x, y, int `z` layer), `TextureComponent` (single `TextureRegion`), `AnimationComponent` (`Animation<TextureRegion>` + elapsed), `VelocityComponent` (dx, dy in px/s), `WrapAroundComponent` (sprite widthPx, marks an entity as horizontally looping), and `InputComponent` (marker for input-driven entities). Systems are `@Singleton` with `@Inject` constructors and contributed via `@Provides @Singleton @IntoSet EntitySystem` providers — scaffold systems in `CoreModule`, demo systems (`BlockSpawnSystem`, `DeathSystem`) in `SampleModule`. `CoreModule.provideEngine(Set<EntitySystem>)` builds a `PooledEngine` and adds each. Ashley's internal priority sort decides per-frame ordering, so set-iteration order doesn't matter. Priorities (lower = earlier): `InputSystem(-10)` drives input-controlled **physics bodies** (family `Input + Body + Position + Texture`) toward the pointer at constant speed via `body.setLinearVelocity(±speed/ppm, 0)`; clamps `pos.x` to `[0, screenW − spriteW]` (using `body.setTransform` to correct out-of-bounds) and snaps with `setTransform` when within one frame's max travel of the target — calibrated-velocity snap doesn't work cleanly with Box2D's fixed timestep. `InputSystem` consults an `InputGate` interface (default `() -> true` from `CoreModule.provideInputGate`); `SampleModule` rebinds it via `@Sample InputGate` to a `GameState`-backed gate (`state::isPlaying`) so the player freezes during DYING/GAME_OVER. `PhysicsSystem(-8)` accumulates dt (capped at 0.25s) and runs Box2D `world.step(1/60, 6, 2)` until the accumulator drains, then writes each body's center back to `PositionComponent` in pixels minus texture half-extents (matching the bottom-left form `RenderSystem` consumes); `MovementSystem(-5)` integrates `VelocityComponent` into `PositionComponent` — the non-physical motion pattern. The drifting libGDX-logo background entity in `SampleGameScreen` is the consumer; the scaffold `GameScreen` builds no Movement-driven entities. `WrapAroundSystem(-4)` runs after Movement and loops drifting entities horizontally so they re-enter on the opposite side once fully off-screen — both it and `WrapAroundComponent` live in scaffold ECS (general-purpose utilities) but are unused by the empty `GameScreen` and only exercised by `SampleGameScreen`. `AnimationSystem(0)` advances elapsed and updates the current frame; `RenderSystem(10)` is a `SortedIteratingSystem` sorted by `PositionComponent.z` ascending (background-to-foreground). `Gdx.input` and `Gdx.graphics` are bound through Dagger (`provideInput`, `provideGraphics`) so systems are constructor-injected and tests can substitute mocks. `TheGame.render()` delegates to the active screen; per-frame ECS work happens inside `engine.update(delta)` calls from `Screen.render`, never directly in `TheGame`.

**Aseprite import pipeline is real.** `./gradlew buildAtlases` runs `exportAseprite` (Aseprite CLI with `--split-tags` → `build/aseprite-frames/<basename>_<tag>_<tagframe>.png`) followed by `packTextures` (libGDX TexturePacker → `assets/atlases/<appName>.atlas` + packed PNG). The `aseprite.bin` path comes from `local.properties` (leading `~` is expanded). It must point at the **executable itself**, not a wrapper script — wrappers that `exec` another process can lose env and silently produce zero files. The pipeline is **not** hooked into the normal build; contributors run it manually after editing `.aseprite` source and commit the regenerated atlas, so CI doesn't need Aseprite. The trailing `_<index>` filenames intentionally line up with TexturePacker's region-index parsing, so `atlas.findRegions("<basename>_<tag>")` returns frames in order, ready for `Animation`.

**YAML config is wired through Dagger.** SnakeYAML loads `assets/config/game.yaml` into `GameConfig` via `ConfigLoader`, which is configured with `BeanAccess.FIELD` so POJOs are plain public fields (no getters/setters). `CoreModule.provideGameConfig` opens the asset with `Gdx.files.internal(...).reader()` and delegates. Adding a new config type means: POJO under `config/`, a `@Provides @Singleton` method that opens its asset, and a `ConfigLoaderTest`-style unit test against a `StringReader`. **Android caveat:** SnakeYAML references `java.beans.*` (absent on Android) — `android/proguard-rules.pro` carries `-dontwarn java.beans.**` and a `-keep class **.config.** { *; }` rule (wildcard so renaming the Java package is safe as long as the leaf package stays named `config`) so R8 doesn't strip reflectively populated fields. Renaming the leaf `config` segment requires updating that pattern.

**Dodging gameplay loop with HP and death.** Lives in `core/.../sample/`; the scaffold has no awareness of it beyond the `@Sample`-qualified optional bindings. `GameState` carries `int hp` (`MAX_HP=5`), `float elapsedSec`, and a `Phase` enum (`PLAYING`/`DYING`/`GAME_OVER`). `BlockSpawnSystem` drops a new dynamic block every 1.5s via `FallingBlockFactory` (random x, slight initial angular velocity) while `state.isPlaying()`. `GameContactListener` decrements `hp` per player↔block hit; on `hp <= 0` it flips phase to `DYING` (further hits ignored). On `DYING`, the `@Sample InputGate` (`state::isPlaying`) closes so `InputSystem` freezes the player (`setLinearVelocity(0,0)` each tick), `BlockSpawnSystem` halts, and `DeathSystem` (priority -6, before AnimationSystem) swaps the player's animation to `player1_Died` (`PlayMode.NORMAL`) and resets elapsed; after `isAnimationFinished`, sets phase to `GAME_OVER` and `SampleGameScreen.render` navigates to `GameOverScreen`. **Critical:** `AnimationSystem` calls `Animation.getKeyFrame(elapsed)` (no looping arg) so each animation's intrinsic playMode is respected — using the two-arg overload would force-loop the Died animation forever. Restart path: `GameOverScreen.tryAgain` invokes `SessionResult.onRetry`, which `SampleGameScreen` populates as `() -> nav.goToSampleGame()`. The hop goes through the navigator's `Optional<Provider<@Sample Screen>>` so it cleanly disappears when `SampleModule` is removed. `SampleGameScreen.show` rebuilds from scratch (clears entities, destroys world bodies, `state.reset()`, `spawner.reset()`, re-installs the `GameContactListener` on the world). `BlockSpawnSystem.timer` is `@Singleton` state so explicit `reset()` is required on session restart — don't rely on construction order.

**Screens are Scene2D-driven.** `TheGame` extends libGDX's `Game` and just bootstraps the Dagger graph + hands off to `ScreenNavigator`. Scaffold screens (`LoadingScreen`, `MainMenuScreen`, `PreferencesScreen`, `LevelPickerScreen`, `GameScreen`, `GameOverScreen`) extend `BaseScreen` (Stage + viewport + boilerplate). The scaffold `GameScreen` is a thin Scene2D placeholder — a hint label and ESC → main menu — that clears entities and destroys Box2D bodies on `show()` so visiting it after the sample doesn't leak simulation state. The dodge demo's playable screen is `sample.SampleGameScreen`, which implements `Screen` directly because its gameplay is Ashley/Box2D-driven, not Scene2D — Scene2D is reserved for menus and the empty placeholder. **Cycle break + lazy load:** every scaffold screen is held as a `Provider<>` in `ScreenNavigator` and built only on first `goToXxx()` — important for cold-start on mobile, where `LoadingScreen` should reach the GPU before any sibling screen does any work. The sample screen sits in an additional `@Sample Optional<Provider<Screen>>` slot — empty when `SampleModule` is absent, populated otherwise — so `LevelPickerScreen` can offer a "Dodge Sample" button only when shipped. Screens still take `Provider<ScreenNavigator>` so Dagger can construct screens before the navigator. The `Game` instance itself is bound into the graph via `@BindsInstance` on the component builder. UI uses `assets/ui/uiskin.json` (bundled libGDX skin), provided as `@Singleton Skin` in `CoreModule`. Adding a screen = new class + new `Provider<>` field + `built` flag on `ScreenNavigator` + new `goToXxx()` + a guarded line in `disposeAll()` (resolving an unvisited provider would build it just to dispose it — and for atlas/texture-dependent screens would also resolve providers that may not be valid yet). **Game-state continuity:** `SampleGameScreen` rebuilds the session on every `show()` (clears entities, destroys bodies, resets `GameState`/spawner, re-installs the contact listener) so retry from `GameOverScreen` always starts fresh.

**Sample is opt-in via `SampleModule`.** The dodge demo lives under `core/.../sample/` and is only wired into the graph because `SampleModule.class` is in `GameComponent.modules`. The scaffold reaches it through a `@BindsOptionalOf @Sample com.badlogic.gdx.Screen` slot in `CoreModule` (filled by `SampleModule`'s `provideSampleScreen(SampleGameScreen)`), so `ScreenNavigator` injects an `Optional<Provider<@Sample Screen>>` and exposes `hasSampleGame()` / `goToSampleGame()`; `LevelPickerScreen` only renders the "Dodge Sample" button when `hasSampleGame()` is true. `SessionResult.onRetry` is a `Runnable` populated by `SampleGameScreen` so `GameOverScreen.tryAgain` doesn't need to know which game it just came from. **Deletion recipe:** delete `core/.../sample/` and remove `SampleModule.class` from `GameComponent.modules`. The optional `Screen` slot resolves to empty, the "Dodge Sample" button disappears, `goToSampleGame()` becomes a no-op, and the empty scaffold `GameScreen` (reached via "Empty Game") is the only level. No scaffold file imports anything from `com.codeheadsystems.game.sample.*` — the deletion is mechanical.

**Box2D wired through Dagger.** `CoreModule.provideWorld` builds a `World` from `config.physics.gravity` after explicit `Box2D.init()` and returns it scaffold-pure (no contact listener installed). `pixelsPerMeter` is the single conversion knob (also from config). The contact listener is sample-side: `SampleGameScreen.startNewSession()` calls `world.setContactListener(contactListener)` on every entry (idempotent) so `provideWorld` doesn't drag a `GameContactListener` import into the scaffold module. `SampleGameScreen.show` (on every entry) tears down any prior session — `engine.removeAllEntities()` + iterate `world.getBodies(...)` and `destroyBody` each — then builds a static ground edge, a kinematic player body sized to the sprite (`InputSystem` drives motion via `setLinearVelocity`/`setTransform`; pushes dynamic blocks on collision without being pushed back), and lets `BlockSpawnSystem` produce dynamic blocks over time via `FallingBlockFactory`. Each body has its Ashley `Entity` set as `userData` so `GameContactListener.beginContact` can look up `InputComponent`/`BlockComponent` markers and flip `GameState` to `DYING` on player↔block hits; `World.dispose()` runs in `TheGame.dispose()` and destroys all bodies it owns. The existing `-keepclassmembers class com.badlogic.gdx.physics.box2d.World { ... }` rules in `android/proguard-rules.pro` cover JNI callbacks under R8. **Testing pattern:** Mockito can't instrument Box2D `World`/`Body` (class init requires JNI), so `PhysicsSystemTest` uses real `World` + real `Body` instances; `gdx-platform:natives-desktop` + `gdx-box2d-platform:natives-desktop` are `testRuntimeOnly`, and `@BeforeAll` calls `GdxNativesLoader.load()` + `Box2D.init()`. Assert against observable state (body or component positions after `engine.update(dt)`), not against mock interactions.

**Tests use JUnit 5 + Mockito.** `core/src/test/java/.../RenderSystemTest` is the pattern: build a `PooledEngine`, register the system with a mocked `SpriteBatch`, add entities, call `engine.update(delta)`, and verify with Mockito `InOrder`. This exercises family matching and component mappers in addition to the system code, so prefer it over invoking system methods directly. Mockito is loaded as an explicit `-javaagent` via the `mockitoAgent` configuration to avoid the deprecated self-attach path on JDK 21+ — reuse that configuration when adding mocking deps.

## Asset list generation

`generateAssetList` (root `build.gradle.kts`) is wired into every non-Android subproject's `processResources`. It recursively walks `assets/` and rewrites `assets/assets.txt`. The runtime relies on this manifest, so **don't hand-edit `assets/assets.txt`** — re-run the build (or `:core:processResources`) after adding/removing asset files.

**Asset registry.** Loadable assets are declared as enum constants implementing the `LoadableAsset` interface (`path`, `type`). The split mirrors the module split: `Asset` (`core/.../assets/Asset.java`, scaffold-only — currently just `LOGO`) and `SampleAsset` (`core/.../sample/SampleAsset.java`, demo-only — currently just `GAME_ATLAS`). Each module contributes its enum's values via a `@Provides @ElementsIntoSet Set<LoadableAsset>` provider; `LoadingScreen` injects the multi-bound `Set<LoadableAsset>` and queues every entry through `AssetManager`. Module providers reference their own enum's `.path` to fetch loaded instances (`CoreModule.provideLogoTexture` → `Asset.LOGO`; `SampleModule.provideTextureAtlas` → `SampleAsset.GAME_ATLAS`). At startup `LoadingScreen` cross-checks each `LoadableAsset.path()` against `AssetManifest` (which parses `assets.txt`) and throws a precise error if anything is missing — so a renamed/deleted file fails fast instead of producing a generic miss midway through loading. Adding a scaffold asset = drop the file under `assets/`, add an `Asset` constant, run `:core:processResources`; for a demo-only asset, add to `SampleAsset` instead. Skin and `config/game.yaml` stay outside the registry on purpose — both are needed to draw the `LoadingScreen` itself, so they're loaded eagerly by `CoreModule`.

## Removing the dodge sample

The falling-blocks demo lives entirely under `core/src/main/java/com/codeheadsystems/game/sample/` (and its tests under `core/src/test/.../sample/`). To strip it out and start your own game:

```bash
git rm -rf core/src/main/java/com/codeheadsystems/game/sample \
           core/src/test/java/com/codeheadsystems/game/sample
```

Then edit `core/.../di/GameComponent.java` and remove `com.codeheadsystems.game.sample.SampleModule.class` from the `@Component(modules = {...})` array — leave only `CoreModule.class`. `./gradlew clean build test` and `:android:minifyReleaseWithR8` should both stay green; `LevelPickerScreen` hides the "Dodge Sample" button automatically (its `@Sample`-qualified `Optional<Provider<Screen>>` resolves to empty), and the empty `GameScreen` remains reachable. Verified by running this exact recipe end-to-end.

## Android natives

`android:copyAndroidNatives` extracts `.so` files from the `natives` configuration into `android/libs/<abi>/` and is hooked onto every `merge*JniLibFolders` task. If you bump `gdxVersion`, ABI-specific natives need to be added to the `natives` dependencies in `android/build.gradle.kts` for each new platform.

## Renaming the project

See the README's "Using this template" section for the canonical checklist. The knobs it points at:

- `extra["appName"]` in root `build.gradle.kts` — drives jar names and Construo bundle naming.
- `eclipse.project.name = "game-template-parent"` in root `build.gradle.kts`; module includes in `settings.gradle.kts`.
- `namespace` / `applicationId` in `android/build.gradle.kts`; activity reference in the `android:run` task; `AndroidManifest.xml`.
- `application.mainClass` in `lwjgl3/build.gradle.kts` and Construo `identifier` for macOS.
- Java package paths under each module's `src/main/java/com/codeheadsystems/game/...`.
