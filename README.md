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
* Sample game includes playing a moving 2D character that is dodging falling blocks, wired up to Box2D.

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
`DaggerGameComponent.create().inject(this)`. The component is built
inside `create()` (not in a static initializer) because providers may
touch GL or `Gdx.files`, which are only valid after libGDX has
initialized.

To add an injected service, add a `@Provides` method to `GameModule`
(or a new `@Module` listed in `GameComponent`) and request it via
constructor injection from the consumer. Annotation processing is
already configured in `core/build.gradle.kts`; generated sources land
under `core/build/generated/sources/annotationProcessor/`.

### Ashley (ECS)

Game state lives in Ashley `Entity` instances composed of pure-data
`Component`s. Behavior lives in `EntitySystem`s.

Layout:

* `ecs/component/` — pure-data components (`PositionComponent`,
  `TextureComponent`).
* `ecs/system/` — behavior. `RenderSystem` extends `IteratingSystem`,
  matches `Family.all(PositionComponent, TextureComponent)`, and draws
  via the injected `SpriteBatch`.

The `Engine` itself is provided by Dagger (`GameModule.provideEngine`),
which constructs a `PooledEngine`, takes any systems as constructor
parameters, and registers them. To add a new system:

1. Annotate it `@Singleton` and give it an `@Inject` constructor — Dagger
   will wire its dependencies.
2. Add it as a parameter on `GameModule.provideEngine` and call
   `engine.addSystem(...)` there. Order = registration order; pass
   priorities to the system constructor if you need deterministic
   ordering.

`TheGame.render()` calls `engine.update(deltaTime)` once per frame; all
per-frame work belongs in a system, not in `TheGame`.

### YAML configuration

Game configuration lives in `assets/config/` and is parsed by
SnakeYAML. The default `assets/config/game.yaml` looks like:

```yaml
title: Game Template
logo:
  x: 140
  y: 210
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

To add a new screen, implement `Screen`, register it with the Dagger
graph, and add a transition from wherever it should be reachable.

## Roadmap

* iOS target (planned once Apple hardware is available)
* Controller input
* Networking layer

## Non-goals

* iOS support today (see Roadmap)
* Engine choice beyond libGDX

## License

MIT — see [LICENSE](LICENSE).
