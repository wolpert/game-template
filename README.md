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

1. Change `eclipse.project.name` and the root project name in `settings.gradle` / `build.gradle`.
2. Change `ext.appName` in the root `build.gradle`.
3. Update the Android `applicationId` and the `package` attribute in `android/AndroidManifest.xml`.
4. Replace launcher icons under `android/res/` and `lwjgl3/icons/`.
5. Replace the sample game assets under `assets/`.
6. Update this README's About section.

## Project layout

```
core/      Shared game code (screens, systems, Dagger graph, Box2D wiring)
lwjgl3/    Desktop launcher (LWJGL3 backend) and OS-specific icons
android/   Android launcher, manifest, resources
assets/    Runtime-loadable content: textures, audio, YAML configs, atlases
art/       Source art (e.g. .aseprite files) — not shipped, exported into assets/
```

### Dagger

Dagger components and modules live under `core/`. To add an injected
service, declare it in the relevant `@Module` and request it via
constructor injection from a screen or system.

### Ashley (ECS)

Game state lives in Ashley `Entity` instances composed of pure-data
`Component`s. Behavior lives in `EntitySystem`s (movement, rendering,
Box2D sync, input, etc.). To add a new behavior, define any new
components, add a system that operates on the matching `Family`, and
register the system with the `Engine` from the Dagger graph.

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
