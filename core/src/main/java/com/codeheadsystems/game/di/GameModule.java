package com.codeheadsystems.game.di;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.codeheadsystems.game.config.ConfigLoader;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.system.RenderSystem;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.io.Reader;
import javax.inject.Singleton;

@Module
public class GameModule {

    private static final String GAME_CONFIG_PATH = "config/game.yaml";

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
    GameConfig provideGameConfig(ConfigLoader loader) {
        try (Reader reader = Gdx.files.internal(GAME_CONFIG_PATH).reader()) {
            return loader.load(GameConfig.class, reader);
        } catch (IOException e) {
            throw new GdxRuntimeException("failed to load " + GAME_CONFIG_PATH, e);
        }
    }

    @Provides
    @Singleton
    Engine provideEngine(RenderSystem renderSystem) {
        Engine engine = new PooledEngine();
        engine.addSystem(renderSystem);
        return engine;
    }
}
