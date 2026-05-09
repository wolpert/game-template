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
* Sample game includes a moving 2D character dodging falling blocks (touch/click to move; 5 hits ends the run after a death animation, score is time survived) wired up to Box2D.

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
5. Move the Java sources under `core/src/main/java/com/codeheadsystems/game/` (and the matching `lwjgl3` / `android` launcher packages) to your own package. The R8 keep rule in `android/proguard-rules.pro` matches any `**.config.**` package, so YAML POJOs continue to deserialize on release Android builds as long as their package still contains a `config` segment — if you rename the config package to something else, update that rule too.
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

* `CoreModule` — scaffold-only `@Provides` bindings: `SpriteBatch`,
  `AssetManager`, `Skin`, `Preferences`, the Box2D `World`, `GameConfig`,
  the scaffold `EntitySystem`s (multibound via `@IntoSet`), the scaffold
  `LoadableAsset` set, and `@BindsOptionalOf @Sample` slots that demo
  code can fill.
* `SampleModule` — the bundled dodge demo: a `TextureAtlas` provider, the
  demo's two extra `EntitySystem`s, the demo `LoadableAsset` set, and
  the `@Sample`-qualified bindings that fill the optional slots
  (`Screen`, `InputGate`, debug-overlay extras).
* `GameComponent` — the `@Singleton @Component` listing both modules and
  exposing `inject(TheGame)`.

The two-module split is the seam that makes the demo opt-in. The
scaffold never imports anything from `com.codeheadsystems.game.sample.*`;
the `@Sample` qualifier (`di/Sample.java`) plus `@BindsOptionalOf` lets
the sample rebind scaffold defaults at the Dagger level. Dropping
`SampleModule.class` from `GameComponent.modules` resolves every
optional slot to empty — see [Removing the dodge sample](#removing-the-dodge-sample)
below.

`TheGame.create()` builds the graph with
`DaggerGameComponent.builder().game(this).build().inject(this)`. The
component is built inside `create()` (not in a static initializer)
because providers may touch GL or `Gdx.files`, which are only valid
after libGDX has initialized. The `Game` instance itself is bound into
the graph via `@BindsInstance` on the component builder so
`ScreenNavigator` can hold it for `setScreen` calls.

To add an injected service, add a `@Provides` method to `CoreModule`
(scaffold-wide service) or a new `@Module` listed in `GameComponent`
and request it via constructor injection from the consumer. Annotation
processing is already configured in `core/build.gradle.kts`; generated
sources land under `core/build/generated/sources/annotationProcessor/`.

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
  * `PlayerComponent` — marker; lets `DeathSystem` find the player without coupling to `InputComponent` (which is about input, not identity).
* `ecs/system/` — behavior, ordered by priority (lower runs earlier).
  * `InputSystem` (priority -10) drives input-controlled **physics bodies** toward the pointer at constant speed and stops them at the pointer or the screen edge. Family is `Input + Body + Position + Texture`; each tick it clamps `pos.x` to `[0, screenW − spriteW]` (correcting via `body.setTransform(...)` if needed), then either sets `body.setLinearVelocity(±speed/ppm, 0)` or, when within one frame's max travel of the target, snaps the body with `setTransform` and zeros velocity. The snap path uses `setTransform` rather than calibrated velocity because Box2D's fixed timestep can't be aligned with the frame's `dt` — for a kinematic body, teleporting cleanly is the right tool. Pointer up clears velocity. Works for desktop mouse and mobile touch interchangeably — libGDX's `Input` interface unifies them.
  * `PhysicsSystem` (priority -8) accumulates frame deltas (capped at 0.25s) and runs Box2D `world.step(1/60s, 6, 2)` as many times as fit, then writes each body's center back to `PositionComponent` in pixels (subtracting the texture's half-extents so the result is bottom-left-anchored, the form `RenderSystem` expects).
  * `MovementSystem` (priority -5) integrates velocity into position (`pos += vel * dt`) for any entity with both components. The current demo doesn't use it (the player uses Box2D for motion), but it's kept as the canonical pattern for non-physical entities.
  * `BlockSpawnSystem` (priority -9) spawns a fresh falling block every 1.5s by delegating to `FallingBlockFactory`; halts when `!GameState.isPlaying()`. `reset()` schedules an immediate next spawn (used on session restart).
  * `DeathSystem` (priority -6) drives the death sequence: when `GameState.phase` is `DYING`, swaps the player's `AnimationComponent.animation` to the one-shot `player1_Died` clip and resets elapsed; on a later tick, when `Animation.isAnimationFinished(elapsed)` returns true, sets phase to `GAME_OVER` so `SampleGameScreen.render` can navigate away. Runs before `AnimationSystem` so the swap takes effect on the same tick the phase changed.
  * `AnimationSystem` (priority 0) advances `elapsed` and writes the current frame into `TextureComponent.region`. Calls `Animation.getKeyFrame(elapsed)` (the one-arg overload) so each animation's intrinsic `PlayMode` is honored — `LOOP` animations loop, `NORMAL` animations stop on the last frame.
  * `RenderSystem` (priority 10) is a `SortedIteratingSystem` keyed on `PositionComponent.z`; the family is `Position + Texture` and it draws via the injected `SpriteBatch`.

`Gdx.input` and `Gdx.graphics` are bound through Dagger (`provideInput`,
`provideGraphics`), so systems take them via constructor injection and
tests can substitute mocks — see `InputSystemTest` for the pattern.

#### Box2D

The Box2D `World` is provided by Dagger (`CoreModule.provideWorld`) and
created with gravity from `config.physics.gravity`. `Box2D.init()` is
called inside the provider so native loading is explicit. The world is
returned scaffold-pure (no contact listener) — the dodge demo's
`SampleGameScreen.startNewSession()` installs its `GameContactListener`
on every entry, which keeps the listener (and its sample-side imports)
out of the core module. The sample screen (on each `show()`) tears down
any prior session and populates the world:

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

`GameState` tracks the session in three explicit phases — `PLAYING`,
`DYING`, `GAME_OVER` — plus an `int hp` (5 to start, in
`GameState.MAX_HP`) and an `elapsedSec` score. The contact listener
decrements `hp` on each player↔block contact while `PLAYING`; when
`hp` reaches 0 it flips the phase to `DYING`. From there:

* `BlockSpawnSystem` and `InputSystem` both gate on
  `state.isPlaying()` — no new blocks spawn, the player freezes in
  place.
* `DeathSystem` swaps in the `player1_Died` animation (with
  `PlayMode.NORMAL`) and watches for `isAnimationFinished`.
* On finish, phase becomes `GAME_OVER`; `SampleGameScreen.render` notices
  and navigates to `GameOverScreen`, which displays the elapsed time
  and offers Try Again / Main Menu.

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

The `Engine` itself is provided by Dagger (`CoreModule.provideEngine`),
which constructs a `PooledEngine` and registers every `EntitySystem`
contributed via Dagger multibindings (`@Provides @IntoSet EntitySystem`
methods in `CoreModule` for scaffold systems and `SampleModule` for
demo systems). Ashley sorts by each system's priority each tick, so
set-iteration order doesn't matter. To add a new system:

1. Annotate it `@Singleton` and give it an `@Inject` constructor — Dagger
   will wire its dependencies. Pass a priority into `super(...)` if you
   need deterministic ordering relative to other systems.
2. Add a `@Provides @Singleton @IntoSet EntitySystem` method in the
   appropriate module (`CoreModule` if it's scaffold; `SampleModule` if
   it's specific to the dodge demo).

The active `Screen.render()` calls `engine.update(deltaTime)` once per
frame; all per-frame gameplay work belongs in a system, not in the
screen.

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
2. Add a `@Provides @Singleton` method to `CoreModule` that opens the
   asset (`Gdx.files.internal("config/your-file.yaml").reader()`) and
   delegates to `ConfigLoader.load(YourConfig.class, reader)`.
3. Constructor- or field-inject the POJO wherever you need it.
4. Cover the parsing in a `ConfigLoaderTest`-style unit test using a
   `StringReader` — no libGDX init needed.

#### Android caveat

SnakeYAML's bean-introspection path references `java.beans.*`, which
doesn't exist on Android. We sidestep that with `-dontwarn java.beans.**`
and a `-keep class **.config.** { *; }` rule in
`android/proguard-rules.pro` (so R8 doesn't strip the reflectively
populated fields). The wildcard matches any package whose path contains
a `config` segment, so the rule survives the rename checklist as long
as your YAML POJOs still live in a `…config…` package. Moving them
elsewhere means updating that keep rule to match.

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

### Assets

Runtime-loadable content lives in `assets/`. The Gradle build scans
that tree during `processResources` and rewrites
`assets/assets.txt` — a flat manifest the runtime cross-checks at load
time. **Don't hand-edit `assets.txt`**; re-run the build (or
`:core:processResources`) after adding or removing files.

Each loadable asset is declared as a constant on a `LoadableAsset`
enum, paired with its `AssetManager` type:

* `assets/Asset.java` — scaffold-only assets (currently just `LOGO`).
* `sample/SampleAsset.java` — dodge-demo assets (currently just
  `GAME_ATLAS`).

Each module contributes its enum's values via
`@Provides @ElementsIntoSet Set<LoadableAsset>`; `LoadingScreen`
injects the multibound `Set<LoadableAsset>`, queues every entry through
`AssetManager`, and validates each path against `AssetManifest` (parsed
from `assets.txt`) so a renamed or deleted file fails fast with a
precise error instead of producing a generic miss midway through
loading. The `Skin` and `config/game.yaml` are loaded eagerly outside
this registry — `LoadingScreen` itself needs them before the manager
runs.

To add a scaffold asset: drop the file under `assets/`, add a constant
to `Asset`, run `:core:processResources` so `assets.txt` regenerates.
For a demo-only asset, add to `SampleAsset` instead.

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
Loading → Main Menu → { Preferences, Level Picker → { Empty Game, Dodge Sample } }
```

`TheGame` extends libGDX's `Game`, builds the Dagger graph, and hands
control to `ScreenNavigator`, which exposes `goToLoading()` /
`goToMainMenu()` / `goToPreferences()` / `goToLevelPicker()` /
`goToGame()` / `goToGameOver()` / `goToSampleGame()`. Each screen is
held as a `Provider<>` and only built on first navigation — important
for cold-start on mobile where `LoadingScreen` should reach the GPU
before any sibling screen does any work. Screens themselves take
`Provider<ScreenNavigator>` to break the construction cycle.

Screens live in `com.codeheadsystems.game.screens`:

* `LoadingScreen` — drives the `AssetManager` and cross-checks every
  declared `LoadableAsset` against `AssetManifest` so a missing or
  renamed file fails fast with a precise error.
* `MainMenuScreen`, `PreferencesScreen`, `LevelPickerScreen`,
  `GameOverScreen` — Scene2D menus extending `BaseScreen` (owns a
  `Stage` + viewport + standard `show/render/resize/dispose`
  boilerplate). `LevelPickerScreen` only renders the "Dodge Sample"
  button when `ScreenNavigator.hasSampleGame()` is true.
* `GameScreen` — the empty scaffold game screen, extending
  `BaseScreen`. A hint label and `ESC` → main menu, with `show()`
  clearing engine entities and destroying all Box2D bodies so the
  placeholder is clean even if it's reached after the dodge demo. This
  is your starting point for new gameplay.
* `sample.SampleGameScreen` — the dodge demo's playable screen, lives
  under the sample package and implements `Screen` directly because its
  gameplay is Ashley/Box2D-driven (Scene2D is reserved for menus + the
  empty placeholder). `show()` rebuilds a session from scratch: clears
  entities, destroys world bodies, resets `GameState` + spawner,
  re-installs `GameContactListener`. Wired into the navigator via
  `@Sample Optional<Provider<Screen>>` so it cleanly disappears when
  `SampleModule` is removed.

UI uses the bundled `assets/ui/uiskin.json` (provided as
`@Singleton Skin` in `CoreModule`). Each menu screen builds its widget
tree in its `@Inject` constructor.

#### Adding a new screen

1. Create the class under `screens/`, extending `BaseScreen` (menus) or
   implementing `Screen` directly (game-style).
2. Annotate `@Singleton`, take `Skin` and `Provider<ScreenNavigator>`
   via `@Inject` constructor — the `Provider` breaks the construction
   cycle between the navigator and its screens.
3. Add a `Provider<>` field + a `built` flag + a `goToXxx()` method on
   `ScreenNavigator`, plus a guarded line in `disposeAll()` so the
   provider isn't resolved (and thus built just to be disposed) if it
   was never visited.

### Removing the dodge sample

The falling-blocks demo lives entirely under
`core/src/main/java/com/codeheadsystems/game/sample/` (with tests under
`core/src/test/.../sample/`). To strip it out and start your own game:

```bash
git rm -rf core/src/main/java/com/codeheadsystems/game/sample \
           core/src/test/java/com/codeheadsystems/game/sample
```

Then edit `core/.../di/GameComponent.java` and remove
`com.codeheadsystems.game.sample.SampleModule.class` from the
`@Component(modules = {...})` array — leave only `CoreModule.class`. No
scaffold file imports anything from `com.codeheadsystems.game.sample.*`,
so the deletion is mechanical: `./gradlew clean build test` and
`:android:minifyReleaseWithR8` should both stay green. The empty
scaffold `GameScreen` (reached via "Empty Game") becomes the only
level; `LevelPickerScreen` automatically hides the "Dodge Sample"
button (its `@Sample`-qualified `Optional<Provider<Screen>>` resolves
to empty) and `goToSampleGame()` becomes a no-op.

## Roadmap

* iOS target (planned once Apple hardware is available)
* Controller input
* Networking layer

## Non-goals

* iOS support today (see Roadmap)
* Engine choice beyond libGDX

## License

MIT — see [LICENSE](LICENSE).
