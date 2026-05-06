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

Game configuration is loaded from YAML files under `assets/config/`.
Add new config types by defining a POJO and binding it in the config
loader module.

### Aseprite import

Source `.aseprite` files live in `art/` and are **not** shipped with the
game. A Gradle task invokes the Aseprite CLI to export each file to a
PNG sheet plus a JSON frame/tag manifest, then runs TexturePacker to
produce atlases under `assets/atlases/`. The runtime animation system
loads those atlases — it never reads `.aseprite` directly.

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
