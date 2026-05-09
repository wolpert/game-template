package com.codeheadsystems.game.di;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.codeheadsystems.game.AppInfo;
import com.codeheadsystems.game.assets.Asset;
import com.codeheadsystems.game.assets.LoadableAsset;
import com.codeheadsystems.game.config.ConfigLoader;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.InputGate;
import com.codeheadsystems.game.ecs.system.AnimationSystem;
import com.codeheadsystems.game.ecs.system.MovementSystem;
import com.codeheadsystems.game.ecs.system.PhysicsSystem;
import com.codeheadsystems.game.ecs.system.RenderSystem;
import com.codeheadsystems.game.ecs.system.WrapAroundSystem;
import com.codeheadsystems.game.highscore.HighscoreReader;
import com.codeheadsystems.game.lifecycle.AppLifecycle;
import com.codeheadsystems.game.lifecycle.LifecycleGate;
import com.codeheadsystems.game.physics.PhysicsWorld;
import com.codeheadsystems.game.render.TintFlash;
import com.codeheadsystems.game.screens.GameOverScreen;
import com.codeheadsystems.game.screens.GameScreen;
import com.codeheadsystems.game.screens.LevelPickerScreen;
import com.codeheadsystems.game.screens.LoadingScreen;
import com.codeheadsystems.game.screens.MainMenuScreen;
import com.codeheadsystems.game.screens.PreferencesScreen;
import dagger.BindsOptionalOf;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import java.io.IOException;
import java.io.Reader;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Singleton;

@Module
public abstract class CoreModule {

    private static final String GAME_CONFIG_PATH = "config/game.yaml";
    private static final String PREFERENCES_NAME = AppInfo.NAME;

    @BindsOptionalOf @Sample abstract InputGate optionalSampleInputGate();

    @BindsOptionalOf @Sample abstract Supplier<String> optionalSampleDebugLine();

    /**
     * Optional sample-side highscore source. {@code MainMenuScreen} injects the
     * {@link Optional} and only renders a "Best:" line when a {@link HighscoreReader} is bound
     * (i.e. {@code SampleModule} is wired in). Removing the demo cleanly drops the line.
     */
    @BindsOptionalOf @Sample abstract HighscoreReader optionalSampleHighscoreReader();

    /**
     * Default scaffold binding: input is always active. Demo / sample modules can rebind
     * this via {@code @Sample}-qualified providers (consumed through the optional slot above)
     * to a gameplay-state-aware impl (e.g. closed during DYING / GAME_OVER) so
     * {@code InputSystem} (in the sample package) stays free of demo-specific deps.
     */
    @Provides
    @Singleton
    static InputGate provideInputGate(@Sample Optional<InputGate> override) {
        return override.orElse(() -> true);
    }

    /**
     * Default scaffold binding for {@link com.codeheadsystems.game.debug.DebugOverlay}'s extras
     * line — empty by default. Demo / sample modules can rebind this {@code Supplier<String>}
     * via the {@code @Sample}-qualified optional slot to surface gameplay state without forcing
     * a demo-specific dependency back into the scaffold overlay.
     */
    @Provides
    @Singleton
    static Supplier<String> provideDebugOverlayExtra(@Sample Optional<Supplier<String>> override) {
        return override.orElseGet(() -> () -> "");
    }

    /**
     * Bind the mutable {@link AppLifecycle} singleton behind the read-only {@link LifecycleGate}
     * interface. {@code TheGame} injects the concrete impl so it can flip the active flag from
     * {@code pause()}/{@code resume()}; everything else takes the gate.
     */
    @Provides
    @Singleton
    static LifecycleGate provideLifecycleGate(AppLifecycle app) {
        return app;
    }

    @Provides
    @Singleton
    static SpriteBatch provideSpriteBatch() {
        return new SpriteBatch();
    }

    @Provides
    @Singleton
    static AssetManager provideAssetManager() {
        return new AssetManager();
    }

    /**
     * Lifecycle foot-gun: throws {@code GdxRuntimeException} if {@link AssetManager} hasn't yet
     * finished loading {@link Asset#LOGO}. Callers must defer the lookup until {@code LoadingScreen}
     * has drained the manager — depend on {@code Provider<Texture>} (or be transitively reachable
     * only from a screen reached after loading), not on {@code Texture} directly.
     */
    @Provides
    @Singleton
    static Texture provideLogoTexture(AssetManager assets) {
        return assets.get(Asset.LOGO.path, Texture.class);
    }

    /**
     * Lifecycle foot-gun: {@link Gdx#app} is null until {@code Lwjgl3Launcher} (or the Android
     * {@code AndroidLauncher}) has constructed the {@link com.badlogic.gdx.Application}. Don't
     * resolve this provider during graph construction — {@code TheGame.create()} runs the Dagger
     * builder, so anything reached from {@code inject(this)} is safe.
     */
    @Provides
    @Singleton
    static Preferences providePreferences() {
        return Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    /**
     * Lifecycle foot-gun: {@link Gdx#files} is null until libGDX has finished platform init in
     * {@code create()}. Same constraint as {@link #providePreferences()} — safe to resolve from
     * inside {@code TheGame.create()} onward, unsafe before.
     */
    @Provides
    @Singleton
    static Skin provideSkin() {
        return new Skin(Gdx.files.internal("ui/uiskin.json"));
    }

    /**
     * Lifecycle-dependent global: {@link Gdx#input} is the live polling singleton, not a snapshot.
     * Captured at injection time, but the underlying object's state changes every frame — never
     * cache derived values across frames. Consumers must run inside a system's {@code update()} or
     * a screen's {@code render()}, never during graph construction.
     */
    @Provides
    @Singleton
    static Input provideInput() {
        return Gdx.input;
    }

    /**
     * Lifecycle-dependent global: {@link Gdx#graphics} is the live frame/window object. Width and
     * height change on resize; FPS and delta change every frame. Same constraint as
     * {@link #provideInput()} — only call into it from per-frame paths.
     */
    @Provides
    @Singleton
    static Graphics provideGraphics() {
        return Gdx.graphics;
    }

    /**
     * Single source of truth for the Box2D world: {@link PhysicsWorld} owns the {@link World} +
     * its {@link com.badlogic.gdx.physics.box2d.ContactListener} as a unit. Raw-world consumers
     * (e.g. {@link PhysicsSystem}) get the world via this delegate — there is one constructed
     * {@code World} per app, regardless of which form a consumer asks for.
     */
    @Provides
    @Singleton
    static World provideWorld(PhysicsWorld physicsWorld) {
        return physicsWorld.getWorld();
    }

    @Provides
    @Singleton
    static GameConfig provideGameConfig(ConfigLoader loader) {
        try (Reader reader = Gdx.files.internal(GAME_CONFIG_PATH).reader()) {
            return loader.load(GameConfig.class, reader);
        } catch (IOException e) {
            throw new GdxRuntimeException("failed to load " + GAME_CONFIG_PATH, e);
        }
    }

    // Add new scaffold systems here:
    @Provides @Singleton @IntoSet
    static EntitySystem bindPhysicsSystem(PhysicsSystem s) { return s; }

    @Provides @Singleton @IntoSet
    static EntitySystem bindMovementSystem(MovementSystem s) { return s; }

    @Provides @Singleton @IntoSet
    static EntitySystem bindWrapAroundSystem(WrapAroundSystem s) { return s; }

    @Provides @Singleton @IntoSet
    static EntitySystem bindAnimationSystem(AnimationSystem s) { return s; }

    @Provides @Singleton @IntoSet
    static EntitySystem bindTintFlashSystem(TintFlash s) { return s; }

    @Provides @Singleton @IntoSet
    static EntitySystem bindRenderSystem(RenderSystem s) { return s; }

    @Provides
    @Singleton
    static Engine provideEngine(Set<EntitySystem> systems) {
        Engine engine = new PooledEngine();
        for (EntitySystem s : systems) {
            engine.addSystem(s);
        }
        return engine;
    }

    // Scaffold's loadable assets — see Asset enum.
    @Provides
    @ElementsIntoSet
    static Set<LoadableAsset> provideScaffoldAssets() {
        return Set.copyOf(EnumSet.allOf(Asset.class));
    }

    // Screen registry. Add a new screen by adding one @IntoMap @ScreenKey provider — no matching
    // field/ctor/method/dispose-line edits in ScreenNavigator are required.
    @Provides @Singleton @IntoMap @ScreenKey(LoadingScreen.class)
    static com.badlogic.gdx.Screen bindLoadingScreen(LoadingScreen s) { return s; }

    @Provides @Singleton @IntoMap @ScreenKey(MainMenuScreen.class)
    static com.badlogic.gdx.Screen bindMainMenuScreen(MainMenuScreen s) { return s; }

    @Provides @Singleton @IntoMap @ScreenKey(PreferencesScreen.class)
    static com.badlogic.gdx.Screen bindPreferencesScreen(PreferencesScreen s) { return s; }

    @Provides @Singleton @IntoMap @ScreenKey(LevelPickerScreen.class)
    static com.badlogic.gdx.Screen bindLevelPickerScreen(LevelPickerScreen s) { return s; }

    @Provides @Singleton @IntoMap @ScreenKey(GameScreen.class)
    static com.badlogic.gdx.Screen bindGameScreen(GameScreen s) { return s; }

    @Provides @Singleton @IntoMap @ScreenKey(GameOverScreen.class)
    static com.badlogic.gdx.Screen bindGameOverScreen(GameOverScreen s) { return s; }
}
