package com.codeheadsystems.game.di;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.codeheadsystems.game.ecs.system.RenderSystem;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class GameModule {

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
    Engine provideEngine(RenderSystem renderSystem) {
        Engine engine = new PooledEngine();
        engine.addSystem(renderSystem);
        return engine;
    }
}
