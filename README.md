# Game Template

## About

This project is intended to be a template used to create games easily by
just cloning the repository.

## Features

* Java 21
* libGDX
* Builds for Android and Desktop by default
* Box2D physics
* Ashley entity-component-system framework
* Dagger used for injection
* YAML support
* Stubbed out screens include loading screen, main menu, preferences and level picker.
* Support for importing aseprite files for animation easily.
* Sample game includes a moving 2D character dodging falling blocks (touch/click to move; one-block-touch ends the run, score is time survived) wired up to Box2D.

## Prerequisites

* JDK 21
* Android SDK (for the Android target)
* Gradle wrapper is included — no system Gradle install needed.

## Quick start

```bash
# Desktop (LWJGL3)
./gradlew lwjgl3:run

# Android (device or emulator attached)
./gradlew android:installDebug

# Desktop distributable jar
./gradlew lwjgl3:dist
```

## Using this template

After cloning, rename the project for your game:

1. Change the root project's Eclipse name (`eclipse.project.name = "game-template-parent"` at the bottom of `build.gradle.kts`) and the included module names in `settings.gradle.kts`.
2. Change `extra["appName"] = "game-template"` in the root `build.gradle.kts` — this drives jar names and bundle identifiers.
3. Update Android `namespace` and `applicationId` in `android/build.gradle.kts`, the `package` attribute in `android/AndroidManifest.xml`, and the `com.codeheadsystems.game/...AndroidLauncher` reference in the `android:run` task.
4. Update the desktop `application.mainClass` in `lwjgl3/build.gradle.kts` (and the matching Construo `identifier` for macOS).
5. Move the Java sources under `core/src/main/java/com/codeheadsystems/game/` (and the matching `lwjgl3` / `android` launcher packages) to your own package.
6. Replace launcher icons under `android/res/` and `lwjgl3/icons/`.
7. Replace the sample game assets under `assets/`.
8. Update this README's About section.

## Project layout

```
core/      Shared game code (screens, systems, Dagger graph, Box2D wiring)
lwjgl3/    Desktop launcher (LWJGL3 backend) and OS-specific icons
android/   Android launcher, manifest, resources
assets/    Runtime-loadable content: textures, audio, YAML configs, atlases
art/       Source art (e.g. .aseprite files) — not shipped, exported into assets/
```

### Dagger

Dagger components and modules live under
`core/src/main/java/com/codeheadsystems/game/di/`:

* `GameModule` — `@Provides` bindings for the shared runtime objects
  (`SpriteBatch`, `Texture`, …).
* `GameComponent` — the `@Singleton @Component` that exposes
  `inject(TheGame)`.

`TheGame.create()` builds the graph with
`DaggerGameComponent.builder().game(this).build().inject(this)`. The
component is built inside `create()` (not in a static initializer)
because providers may touch GL or `Gdx.files`, which are only valid
after libGDX has initialized. The `Game` instance itself is bound into
the graph via `@BindsInstance` on the component builder so
`ScreenNavigator` can hold it for `setScreen` calls.

To add an injected service, add a `@Provides` method to `GameModule`
(or a new `@Module` listed in `GameComponent`) and request it via
constructor injection from the consumer. Annotation processing is
already configured in `core/build.gradle.kts`; generated sources land
under `core/build/generated/sources/annotationProcessor/`.

### Ashley (ECS)

Game state lives in Ashley `Entity` instances composed of pure-data
`Component`s. Behavior lives in `EntitySystem`s.

Layout:

* `ecs/component/` — pure-data components.
  * `PositionComponent` — `float x`, `float y`, and `int z` (render layer; lower draws first).
  * `TextureComponent` — a `TextureRegion`, so atlas frames and stand-alone textures share one render path. Wrap a whole `Texture` with `new TextureRegion(texture)`.
  * `AnimationComponent` — a libGDX `Animation<TextureRegion>` plus `float elapsed`.
  * `VelocityComponent` — `float dx`, `float dy` (pixels/second).
  * `InputComponent` — marker; tagged entities read pointer input each tick.
  * `BodyComponent` — wraps a Box2D `Body`. `PhysicsSystem` writes the body's center back to `PositionComponent` (in pixels, as the texture's bottom-left) each tick.
  * `BlockComponent` — marker; identifies falling-block entities so the contact listener can recognize player↔block hits.
* `ecs/system/` — behavior, ordered by priority (lower runs earlier).
  * `InputSystem` (priority -10) drives input-controlled **physics bodies** toward the pointer at constant speed and stops them at the pointer or the screen edge. Family is `Input + Body + Position + Texture`; each tick it clamps `pos.x` to `[0, screenW − spriteW]` (correcting via `body.setTransform(...)` if needed), then either sets `body.setLinearVelocity(±speed/ppm, 0)` or, when within one frame's max travel of the target, snaps the body with `setTransform` and zeros velocity. The snap path uses `setTransform` rather than calibrated velocity because Box2D's fixed timestep can't be aligned with the frame's `dt` — for a kinematic body, teleporting cleanly is the right tool. Pointer up clears velocity. Works for desktop mouse and mobile touch interchangeably — libGDX's `Input` interface unifies them.
  * `PhysicsSystem` (priority -8) accumulates frame deltas (capped at 0.25s) and runs Box2D `world.step(1/60s, 6, 2)` as many times as fit, then writes each body's center back to `PositionComponent` in pixels (subtracting the texture's half-extents so the result is bottom-left-anchored, the form `RenderSystem` expects).
  * `MovementSystem` (priority -5) integrates velocity into position (`pos += vel * dt`) for any entity with both components. The current demo doesn't use it (the player uses Box2D for motion), but it's kept as the canonical pattern for non-physical entities.
  * `BlockSpawnSystem` (priority -9) spawns a fresh falling block every 1.5s by delegating to `FallingBlockFactory`; halts when `GameState.gameOver` is true. `reset()` schedules an immediate next spawn (used on session restart).
  * `AnimationSystem` (priority 0) advances `elapsed` and writes the current frame into `TextureComponent.region`.
  * `RenderSystem` (priority 10) is a `SortedIteratingSystem` keyed on `PositionComponent.z`; the family is `Position + Texture` and it draws via the injected `SpriteBatch`.

`Gdx.input` and `Gdx.graphics` are bound through Dagger (`provideInput`,
`provideGraphics`), so systems take them via constructor injection and
tests can substitute mocks — see `InputSystemTest` for the pattern.

#### Box2D

The Box2D `World` is provided by Dagger (`GameModule.provideWorld`) and
created with gravity from `config.physics.gravity`. `Box2D.init()` is
called inside the provider so native loading is explicit, and a
`GameContactListener` is wired in at the same time. `GameScreen` (on
each `show()`) tears down any prior session and populates the world:

* A static `EdgeShape` **ground** at `y = 0` spanning the screen.
* A kinematic `PolygonShape` **player** sized to the
  `player1_Flying` sprite. Kinematic means `InputSystem` drives it via
  `setLinearVelocity`/`setTransform`; the player pushes blocks on
  contact but isn't pushed back by them.
* Dynamic **falling blocks** spawned over time by `BlockSpawnSystem`
  via `FallingBlockFactory` — animated `block_block` atlas region,
  random x, slight angular velocity, restitution `0.5`. Each block's
  `Body.userData` holds its `Entity` so the contact listener can match
  player↔block via component markers (`InputComponent` vs
  `BlockComponent`).

When the contact listener detects a player↔block touch it sets
`GameState.gameOver = true`; `GameScreen.render` notices on the next
frame and navigates to `GameOverScreen`, which displays the elapsed
time and offers Try Again / Main Menu.

Pixel ↔ meter conversion is centralized: every body is built using
`config.physics.pixelsPerMeter`, and `PhysicsSystem` does the inverse
when syncing positions back. Don't construct bodies in pixel coords —
divide by `pixelsPerMeter` first.

`World.dispose()` runs in `TheGame.dispose()`, which destroys every
body it owns — you don't need to track bodies separately for cleanup.
The existing R8 keep rules in `android/proguard-rules.pro` already
preserve the Box2D JNI callback methods on `World`.

#### Testing physics

`PhysicsSystemTest` constructs a real `World` and real `Body` instances
rather than mocking them — Mockito can't instrument `World` because its
class init touches JNI, and even if it could the resulting tests would
duplicate the Box2D integration in mocks. The native libs are pulled in
as `testRuntimeOnly` (`gdx-platform:natives-desktop` and
`gdx-box2d-platform:natives-desktop`) and `@BeforeAll` calls
`GdxNativesLoader.load()` plus `Box2D.init()`. New physics tests should
follow the same pattern: real World, real Bodies, observable state
(positions after `engine.update(dt)`) as the assertion target.

The `Engine` itself is provided by Dagger (`GameModule.provideEngine`),
which constructs a `PooledEngine`, takes any systems as constructor
parameters, and registers them. To add a new system:

1. Annotate it `@Singleton` and give it an `@Inject` constructor — Dagger
   will wire its dependencies.
2. Add it as a parameter on `GameModule.provideEngine` and call
   `engine.addSystem(...)` there. Order = registration order; pass
   priorities to the system constructor if you need deterministic
   ordering.

`GameScreen.render()` calls `engine.update(deltaTime)` once per frame
(while it's the active screen); all per-frame gameplay work belongs in
a system, not in the screen.

### YAML configuration

Game configuration lives in `assets/config/` and is parsed by
SnakeYAML. The default `assets/config/game.yaml` looks like:

```yaml
title: Game Template
logo:
  x: 140
  y: 210
player:
  speed: 200
physics:
  gravity:
    x: 0
    y: -9.8
  pixelsPerMeter: 32
```

`ConfigLoader` (in `core/src/main/java/.../config/`) is a thin wrapper
that constructs SnakeYAML with `BeanAccess.FIELD`, so config POJOs use
plain public fields rather than getters/setters. Unknown keys fail
loudly so typos in YAML keys surface as errors instead of silently
producing default values.

To add a new config type:

1. Define a POJO under `com.codeheadsystems.game.config` with public
   fields matching the YAML keys.
2. Add a `@Provides @Singleton` method to `GameModule` that opens the
   asset (`Gdx.files.internal("config/your-file.yaml").reader()`) and
   delegates to `ConfigLoader.load(YourConfig.class, reader)`.
3. Constructor- or field-inject the POJO wherever you need it.
4. Cover the parsing in a `ConfigLoaderTest`-style unit test using a
   `StringReader` — no libGDX init needed.

#### Android caveat

SnakeYAML's bean-introspection path references `java.beans.*`, which
doesn't exist on Android. We sidestep that with `-dontwarn java.beans.**`
and a `-keep` rule on `com.codeheadsystems.game.config.**` in
`android/proguard-rules.pro` (so R8 doesn't strip the reflectively
populated fields). If you move config POJOs to a different package,
update that keep rule to match.

### Tests

Tests live under `core/src/test/java/...`, mirroring the package they
exercise. The runner is JUnit 5; mocking via Mockito.

```bash
./gradlew :core:test                             # run all
./gradlew :core:test --tests "com.codeheadsystems.game.ecs.system.RenderSystemTest"
./gradlew :core:test --tests "*RenderSystem*"    # glob match
```

`RenderSystemTest` is the canonical example: it builds a `PooledEngine`
in-process, registers the system under test against a mocked
`SpriteBatch`, adds entities, calls `engine.update(delta)`, and verifies
the resulting batch interactions with Mockito's `InOrder`. Use the same
pattern for new systems — exercise the engine, not the system class
directly, so family matching and component mappers are also covered.

Mockito is wired as an explicit `-javaagent` (configured in
`core/build.gradle.kts`) rather than relying on its self-attach
fallback, which is being removed from future JDKs. New mocking
dependencies should reuse the existing `mockitoAgent` configuration.

### Aseprite import

Source `.aseprite` files live in `art/` and are **not** shipped with the
game. A two-step Gradle pipeline turns them into a libGDX
`TextureAtlas` under `assets/atlases/`, which is what the runtime loads.

#### Configure the Aseprite binary

The pipeline needs a path to the Aseprite executable. Set
`aseprite.bin` in `local.properties` (which is gitignored):

```properties
aseprite.bin=/home/you/programs/aseprite/current
```

A leading `~` is expanded to your home directory. **Point at the
binary itself**, not a wrapper script — wrappers that `exec` another
process can lose the working environment when invoked by Gradle and
silently produce no output.

#### Run the pipeline

```bash
./gradlew buildAtlases       # exportAseprite -> packTextures
./gradlew exportAseprite     # only the .aseprite -> per-frame PNG step
./gradlew packTextures       # only the TexturePacker repack step
```

* `exportAseprite` invokes the Aseprite CLI on every `*.aseprite` in
  `art/` with `--split-tags` and writes one PNG per tag-frame to
  `build/aseprite-frames/`. Filename pattern:
  `<basename>_<tag>_<tagframe>.png`.
* `packTextures` runs libGDX's `TexturePacker` on those frames and
  emits `assets/atlases/<appName>.atlas` plus the packed PNG(s).
  TexturePacker treats the trailing `_<index>` as the region index, so
  a `Walk` tag with three frames becomes regions named `<basename>_Walk`
  with indices 0, 1, 2 — exactly what `atlas.findRegions(...)` returns.

#### When to run it

`buildAtlases` is **not** wired into the regular build — it only runs
when you ask. The convention is: contributors who edit `.aseprite`
files run it locally and commit the regenerated `assets/atlases/`
output. CI machines never need Aseprite installed.

#### Loading at runtime

```java
TextureAtlas atlas = new TextureAtlas("atlases/game-template.atlas");
Array<TextureAtlas.AtlasRegion> walkFrames = atlas.findRegions("player1_Walk");
Animation<TextureRegion> walk = new Animation<>(0.1f, walkFrames, Animation.PlayMode.LOOP);
```

### Screen flow

```
Loading → Main Menu → { Preferences, Level Picker → Game }
```

`TheGame` extends libGDX's `Game` and just builds the Dagger graph,
then hands control to `ScreenNavigator`, which exposes
`goToLoading()` / `goToMainMenu()` / `goToPreferences()` /
`goToLevelPicker()` / `goToGame()`. Screens live in
`com.codeheadsystems.game.screens`:

* `LoadingScreen` — placeholder with a fixed-duration delay; replace
  the timer with an `AssetManager` poll when you're ready for real
  loading.
* `MainMenuScreen`, `PreferencesScreen`, `LevelPickerScreen`,
  `GameOverScreen` — Scene2D menus extending `BaseScreen`, which owns a
  `Stage` + viewport and the standard `show/render/resize/dispose`
  boilerplate.
* `GameScreen` — also extends `BaseScreen` (uses the stage for an HUD
  score label) but overrides `render` to draw the engine first, then
  the HUD on top. `show()` starts a fresh session: clears engine
  entities, destroys all world bodies, resets `GameState` and the spawn
  timer, then rebuilds ground + player. Watches for `gameState.gameOver`
  to navigate to `GameOverScreen`, and `ESC` to return to the main menu.

UI uses the bundled `assets/ui/uiskin.json` (provided as
`@Singleton Skin` in `GameModule`). Each menu screen builds its widget
tree in its `@Inject` constructor.

#### Adding a new screen

1. Create the class under `screens/`, extending `BaseScreen` (for menu
   UI) or implementing `Screen` directly (for game-style screens).
2. Annotate `@Singleton`, take `Skin` and `Provider<ScreenNavigator>`
   via `@Inject` constructor — the `Provider` is required to break the
   construction cycle between the navigator and its screens.
3. Add a field + `goToXxx()` method to `ScreenNavigator`, plus a line
   in `disposeAll()` so its `Stage` is released at app shutdown.

## Roadmap

* iOS target (planned once Apple hardware is available)
* Controller input
* Networking layer

## Non-goals

* iOS support today (see Roadmap)
* Engine choice beyond libGDX

## License

MIT — see [LICENSE](LICENSE).
