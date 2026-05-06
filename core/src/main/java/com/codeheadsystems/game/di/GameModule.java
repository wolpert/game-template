package com.codeheadsystems.game.di;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.codeheadsystems.game.config.ConfigLoader;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.system.AnimationSystem;
import com.codeheadsystems.game.ecs.system.InputSystem;
import com.codeheadsystems.game.ecs.system.MovementSystem;
import com.codeheadsystems.game.ecs.system.RenderSystem;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.io.Reader;
import javax.inject.Singleton;

@Module
public class GameModule {

    private static final String GAME_CONFIG_PATH = "config/game.yaml";
    private static final String GAME_ATLAS_PATH = "atlases/game-template.atlas";

    @Provides
    @Singleton
    SpriteBatch provideSpriteBatch() {
        return new SpriteBatch();
    }

    @Provides
    @Singleton
    Texture provideLogoTexture() {
        return new Texture("libgdx.png");
    }

    @Provides
    @Singleton
    TextureAtlas provideTextureAtlas() {
        return new TextureAtlas(GAME_ATLAS_PATH);
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
                         MovementSystem movementSystem,
                         AnimationSystem animationSystem,
                         RenderSystem renderSystem) {
        Engine engine = new PooledEngine();
        engine.addSystem(inputSystem);
        engine.addSystem(movementSystem);
        engine.addSystem(animationSystem);
        engine.addSystem(renderSystem);
        return engine;
    }
}
