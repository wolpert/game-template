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

    // Sourced from AssetManager — LoadingScreen queues + drains the manager before any consumer
    // (currently only GameScreen, deferred via Provider in ScreenNavigator) resolves these.
    @Provides
    @Singleton
    Texture provideLogoTexture(AssetManager assets) {
        return assets.get(Asset.LOGO.path, Texture.class);
    }

    @Provides
    @Singleton
    TextureAtlas provideTextureAtlas(AssetManager assets) {
        return assets.get(Asset.GAME_ATLAS.path, TextureAtlas.class);
    }

    @Provides
    @Singleton
    Preferences providePreferences() {
        return Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    @Provides
    @Singleton
    Skin provideSkin() {
        return new Skin(Gdx.files.internal("ui/uiskin.json"));
    }

    @Provides
    @Singleton
    Input provideInput() {
        return Gdx.input;
    }

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
                         BlockSpawnSystem blockSpawnSystem,
                         DeathSystem deathSystem,
                         AnimationSystem animationSystem,
                         RenderSystem renderSystem) {
        Engine engine = new PooledEngine();
        engine.addSystem(inputSystem);
        engine.addSystem(physicsSystem);
        engine.addSystem(movementSystem);
        engine.addSystem(blockSpawnSystem);
        engine.addSystem(deathSystem);
        engine.addSystem(animationSystem);
        engine.addSystem(renderSystem);
        return engine;
    }
}
