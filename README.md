# Game Template

## About

This project is intended to be a template used to create games easily by
just cloning the repository.

## Features

* Java 21
* libGDX
* Builds for Android and Desktop by default
* Box2D physics
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
# Desktop
./gradlew desktop:run

# Android (device or emulator attached)
./gradlew android:installDebug

# Desktop distributable jar
./gradlew desktop:dist
```

## Using this template

After cloning, rename the project for your game:

1. Change the root project name in `settings.gradle`.
2. Change `appName` and the application package in `build.gradle`.
3. Update the Android `applicationId` and the `package` attribute in `android/AndroidManifest.xml`.
4. Replace launcher icons and splash assets under `android/res/` and `desktop/`.
5. Replace the sample game assets under `core/assets/`.
6. Update this README's About section.

## Project layout

```
core/        Shared game code (screens, systems, Dagger graph, Box2D wiring)
desktop/     LWJGL3 launcher
android/     Android launcher, manifest, resources
core/assets/ Textures, audio, YAML configs, aseprite sources
```

### Dagger

Dagger components and modules live under `core/`. To add an injected
service, declare it in the relevant `@Module` and request it via
constructor injection from a screen or system.

### YAML configuration

Game configuration is loaded from YAML files under `core/assets/config/`.
Add new config types by defining a POJO and binding it in the config
loader module.

### Aseprite import

Drop `.aseprite` files into `core/assets/aseprite/`. The import step
generates atlases and animation metadata that the animation system
consumes at runtime.

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
