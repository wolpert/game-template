# TODO

Stack-ranked from a four-persona review (indie hobbyist, small-studio lead, Java
backend convert, mobile-first developer). Each item notes which persona(s)
raised it. Conflicts between personas are called out explicitly so they can be
decided rather than silently resolved.

## Tier 1 — Cross-persona consensus, low effort, high payoff

1. ~~**Strip unused dependencies from `core/build.gradle.kts`.** Remove `gdx-ai`,
   `box2dlights`, `gdx-vfx-core`, `gdx-vfx-effects`, `libgdx-utils`,
   `libgdx-utils-box2d`. CLAUDE.md already admits they're unused. Cuts APK size
   and CVE surface. (Sam, Priya, Marcus)~~ **Done** — six `api` deps and their
   six version properties removed; CLAUDE.md note updated; all modules compile,
   tests green.

2. **Separate the demo game from the template scaffolding.** Move
   `FallingBlockFactory`, `BlockComponent`, `BlockSpawnSystem`, `DeathSystem`,
   `GameContactListener`, `GameState`, `GameOverScreen`, and the
   demo-specific bits of `GameScreen` under a `core/.../sample/` package (or
   a separate Gradle source set). A new user can then delete the sample
   wholesale without a scavenger hunt. (Sam, Priya, Marcus)
   - **Conflict:** Sam wants to *delete* the demo; Marcus wants a *richer*
     demo (a `MovementSystem` consumer, e.g. a drifting cloud). Decision
     needed: keep the demo isolated-but-expanded, or strip it to the bone.

3. **Add a debug/perf overlay** (toggle with F3 / a long-press on mobile):
   FPS, frame-time histogram, entity count, `world.getBodyCount()`, current
   `GameState.phase`, GC count. One overlay solves three different
   complaints: Marcus wants it for learning, Priya needs it to catch perf
   regressions on a 12-month project, Jordan needs it for mid-range Android
   tuning. (Marcus, Priya, Jordan)

## Tier 2 — Template ergonomics

4. **Rename automation.** Replace the 8-step "Using this template" checklist
   with a `./scripts/rename.sh` (or Gradle task) that takes
   `--package com.foo.mygame --name MyGame` and rewrites `appName`,
   `eclipse.project.name`, `settings.gradle.kts` includes, Android
   `namespace`/`applicationId`/manifest, the `android:run` activity ref,
   `application.mainClass`, Construo `identifier`, and Java package
   directories in one shot. (Sam, Priya)

5. **Lazy-load all menu screens** via `Provider<>` in `ScreenNavigator`, not
   just `GameScreen`. Currently every screen builds its Scene2D tree in its
   `@Inject` ctor before `LoadingScreen` even draws — bad for cold-start on
   mobile. (Priya)

6. **Centralize the asset manifest.** Replace the scattered string-literal
   paths (`GAME_ATLAS_PATH`, `LOGO_TEXTURE_PATH`, the manual queue in
   `LoadingScreen.show()`) with a single `Assets.java` enum or YAML manifest
   that `LoadingScreen` iterates. Scales as the asset list grows. (Priya)

7. **Annotate the lifecycle foot-guns in code, not just CLAUDE.md.** Javadoc
   on `GameModule.provideTextureAtlas` / `provideLogoTexture` saying "throws
   if called before `LoadingScreen` drains AssetManager — keep consumers
   `Provider<>`-deferred." Same treatment for the `Gdx.input`/`Gdx.graphics`
   providers (lifecycle-dependent globals). (Marcus)

8. **Document or remove `MovementSystem`.** It's registered in `provideEngine`
   but no entity uses it. Either add a sample consumer (a parallax cloud)
   or delete it and document the velocity-integration pattern in a comment
   on `VelocityComponent`. (Sam wants it gone; Marcus wants it demonstrated.)
   - **Conflict:** ties to #2 — same delete-vs-expand axis.

## Tier 3 — Commercial-shipping readiness (mostly Priya + Jordan)

9. **Android signing + AAB pipeline.** Add `signingConfigs` driven by
   `~/.gradle/keystore.properties` (or GH Actions secrets), enable
   `bundle { ... }` with ABI splits, run `./gradlew android:bundleRelease` in
   CI on tags, upload AAB as artifact. Currently `release` has minify on but
   no signing config — Play Console blocks day one. (Priya, Jordan)

10. **CI matrix + caching + artifact upload.** Extend `.github/workflows/ci.yml`
    to ubuntu + macOS + windows runners; cache `~/.gradle/caches` and
    `~/.android`; upload `lwjgl3:dist`, `construo`, and signed
    `android:bundleRelease` outputs on tagged builds. (Priya)

11. **Static analysis + formatting + coverage gate.** Add Spotless
    (google-java-format), ErrorProne + NullAway, JaCoCo with a 70% floor on
    `core/` enforced in CI. Three authors over a year without enforced style
    is merge hell. (Priya)

12. **Crash reporting hook in `AndroidLauncher`.** Stub Sentry or Firebase
    Crashlytics (config via `local.properties`, no-op when absent). Same
    hook on desktop via the Sentry Java SDK. (Jordan)

13. **Notarized macOS bundles.** Construo bundles aren't notarized; Apple
    Gatekeeper will block them. Wire `codesign` + `notarytool` into the
    `lwjgl3:construo` task or document the post-build step. (Priya)

## Tier 4 — Mobile polish (Jordan)

14. **DP-aware viewport / HUD scaling.** Replace `ScreenViewport` in
    `BaseScreen` with `FitViewport` (or world-units viewport) and scale the
    HUD by DP. Tablet vs. phone currently render UI at wildly different
    sizes.
    - **Conflict:** `ScreenViewport` is reasonable for desktop where 1px =
      1unit is intuitive. Either pick `FitViewport` everywhere, or make
      viewport choice per-screen, or expose it in `game.yaml`.

15. **Multitouch + `GestureDetector` example.** `InputSystem` reads first
    pointer only. Add a gesture-detector path alongside the pointer path so
    template users see how to wire pinch/fling/long-press.

16. **Safe-area / notch / cutout handling.** Add
    `android:windowLayoutInDisplayCutoutMode="shortEdges"` to the manifest
    and route insets to the HUD layout.

17. **`pause()` that actually pauses the simulation.** `BaseScreen.pause()` is
    empty. On a phone call or app-backgrounding, the Box2D world keeps
    accumulating dt and the player keeps moving. Pause the engine + zero
    the accumulator on `pause()`, restore on `resume()`.

18. **AAB ABI splits.** Even with #9, a single fat AAB ships every ABI to
    every user. Configure `splits { abi { ... } }` so Play Store delivers
    the right native libs per device.

19. **Portrait-orientation example screen.** `screenOrientation="landscape"`
    is hardcoded in the manifest. Document and demo a portrait path so
    template users don't assume landscape-only.

20. **`backup_rules.xml` + `data_extraction_rules.xml`.** Manifest sets
    `allowBackup="true"` and `fullBackupContent="true"` but no rules file
    exists. Play Console will warn on `targetSdk = 35`.

21. **App-icon refresh.** `ic_launcher-web.png` is the Android Studio relic.
    Replace launcher icons under `android/res/` with adaptive icons (or
    document the swap as a template-rename step).

## Tier 5 — Learning + onboarding docs (Marcus, Sam)

22. **`docs/where-to-start.md`** that maps one frame: input poll →
    `engine.update(dt)` → system priority chain → `SpriteBatch.begin/draw/end`.
    Link from the README. (Marcus)

23. **`CONTRIBUTING.md`** with: Dagger graph diagram, ECS priority table,
    "do not inject `ScreenNavigator` directly into a screen" rule, asset
    pipeline workflow for artists. (Priya)

24. **Label exemplar tests.** Add a header comment on `RenderSystemTest`,
    `PhysicsSystemTest`, `InputSystemTest` declaring each as the canonical
    pattern for "sorted iterating system / fixed-timestep system /
    input-driven system." (Marcus)

25. **A `minimal/` branch or `examples/bare/` source set** with one screen,
    one sprite, no Dagger, no Ashley. Lets a hobbyist start small and
    graduate into the full graph. (Sam)
    - **Conflict (philosophical):** Sam thinks the template is over-engineered
      for hobby work; Priya/Marcus think the rigor *is* the value. A
      minimal branch satisfies both — but doubles maintenance surface.
      Decision needed: lean into rigor (skip #25), lean into accessibility
      (add #25), or split into two repos.

## Tier 6 — Roadmap stretch

26. **Stub RoboVM iOS module.** A skeleton `ios/build.gradle.kts` +
    `IOSLauncher` so the README's iOS roadmap line is more than aspirational.
    Don't have to actually build it on CI — just prove the architecture
    survives a third launcher. (Jordan)

27. **Gradle daemon decision.** `org.gradle.daemon=false` is intentional
    (CLAUDE.md says don't fix it) but every persona that did dev work
    locally complained. Either keep it off and document *why* in
    `gradle.properties` itself, or flip it on and accept the CI startup
    cost.
    - **Conflict:** Sam + Priya want it on for local dev; current
      project policy is off. The fix may be a comment in
      `gradle.properties` explaining the trade-off, plus a documented
      `~/.gradle/gradle.properties` opt-in for local dev.

## Cross-cutting conflict summary

These need a top-level decision before the items below them can be safely
implemented:

- **A. Lean vs. rigorous.** Items #2, #8, #25 all hinge on whether this
  template is for hobbyists (lean) or commercial teams (rigorous), or
  whether it tries to be both via a `minimal` branch.
- **B. Demo: delete-ready vs. demo-rich.** Items #2 and #8 both. Sam wants
  to `rm -rf` the demo; Marcus wants more demo content. Isolating the demo
  under `sample/` (so it's deletable) *and* expanding it (more example
  systems) is the only path that satisfies both.
- **C. Viewport choice.** Item #14. `ScreenViewport` is desktop-friendly,
  `FitViewport` is mobile-friendly. Pick a default or expose it.
- **D. Gradle daemon.** Item #27. Off-by-policy vs. on-by-developer-pain.
