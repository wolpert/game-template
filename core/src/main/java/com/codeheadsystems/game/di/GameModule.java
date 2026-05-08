package com.codeheadsystems.game.di;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.codeheadsystems.game.assets.Asset;
import com.codeheadsystems.game.config.ConfigLoader;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.system.AnimationSystem;
import com.codeheadsystems.game.ecs.system.BlockSpawnSystem;
import com.codeheadsystems.game.ecs.system.DeathSystem;
import com.codeheadsystems.game.ecs.system.InputSystem;
import com.codeheadsystems.game.ecs.system.MovementSystem;
import com.codeheadsystems.game.ecs.system.PhysicsSystem;
import com.codeheadsystems.game.ecs.system.RenderSystem;
import com.codeheadsystems.game.ecs.system.WrapAroundSystem;
import com.codeheadsystems.game.session.GameContactListener;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.io.Reader;
import javax.inject.Singleton;

@Module
public class GameModule {

    private static final String GAME_CONFIG_PATH = "config/game.yaml";
    private static final String PREFERENCES_NAME = "game-template";

    @Provides
    @Singleton
    SpriteBatch provideSpriteBatch() {
        return new SpriteBatch();
    }

    @Provides
    @Singleton
    AssetManager provideAssetManager() {
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
    Texture provideLogoTexture(AssetManager assets) {
        return assets.get(Asset.LOGO.path, Texture.class);
    }

    /**
     * Lifecycle foot-gun: throws {@code GdxRuntimeException} if {@link AssetManager} hasn't yet
     * finished loading {@link Asset#GAME_ATLAS}. See {@link #provideLogoTexture} — same {@code Provider}
     * deferral applies.
     */
    @Provides
    @Singleton
    TextureAtlas provideTextureAtlas(AssetManager assets) {
        return assets.get(Asset.GAME_ATLAS.path, TextureAtlas.class);
    }

    /**
     * Lifecycle foot-gun: {@link Gdx#app} is null until {@code Lwjgl3Launcher} (or the Android
     * {@code AndroidLauncher}) has constructed the {@link com.badlogic.gdx.Application}. Don't
     * resolve this provider during graph construction — {@code TheGame.create()} runs the Dagger
     * builder, so anything reached from {@code inject(this)} is safe.
     */
    @Provides
    @Singleton
    Preferences providePreferences() {
        return Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    /**
     * Lifecycle foot-gun: {@link Gdx#files} is null until libGDX has finished platform init in
     * {@code create()}. Same constraint as {@link #providePreferences()} — safe to resolve from
     * inside {@code TheGame.create()} onward, unsafe before.
     */
    @Provides
    @Singleton
    Skin provideSkin() {
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
    Input provideInput() {
        return Gdx.input;
    }

    /**
     * Lifecycle-dependent global: {@link Gdx#graphics} is the live frame/window object. Width and
     * height change on resize; FPS and delta change every frame. Same constraint as
     * {@link #provideInput()} — only call into it from per-frame paths.
     */
    @Provides
    @Singleton
    Graphics provideGraphics() {
        return Gdx.graphics;
    }

    @Provides
    @Singleton
    World provideWorld(GameConfig config, GameContactListener listener) {
        Box2D.init(); // idempotent; safer than relying on lazy native loading.
        World world = new World(new Vector2(config.physics.gravity.x, config.physics.gravity.y), /*doSleep=*/ true);
        world.setContactListener(listener);
        return world;
    }

    @Provides
    @Singleton
    GameConfig provideGameConfig(ConfigLoader loader) {
        try (Reader reader = Gdx.files.internal(GAME_CONFIG_PATH).reader()) {
            return loader.load(GameConfig.class, reader);
        } catch (IOException e) {
            throw new GdxRuntimeException("failed to load " + GAME_CONFIG_PATH, e);
        }
    }

    @Provides
    @Singleton
    Engine provideEngine(InputSystem inputSystem,
                         PhysicsSystem physicsSystem,
                         MovementSystem movementSystem,
                         WrapAroundSystem wrapAroundSystem,
                         BlockSpawnSystem blockSpawnSystem,
                         DeathSystem deathSystem,
                         AnimationSystem animationSystem,
                         RenderSystem renderSystem) {
        Engine engine = new PooledEngine();
        engine.addSystem(inputSystem);
        engine.addSystem(physicsSystem);
        engine.addSystem(movementSystem);
        engine.addSystem(wrapAroundSystem);
        engine.addSystem(blockSpawnSystem);
        engine.addSystem(deathSystem);
        engine.addSystem(animationSystem);
        engine.addSystem(renderSystem);
        return engine;
    }
}
