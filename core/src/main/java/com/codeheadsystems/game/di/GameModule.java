package com.codeheadsystems.game.di;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
}
