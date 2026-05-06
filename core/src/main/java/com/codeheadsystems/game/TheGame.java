package com.codeheadsystems.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codeheadsystems.game.di.DaggerGameComponent;
import javax.inject.Inject;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TheGame extends ApplicationAdapter {
    @Inject SpriteBatch batch;
    @Inject Texture image;

    @Override
    public void create() {
        // Build the Dagger graph here — providers may touch GL/Gdx, which is only valid after create().
        DaggerGameComponent.create().inject(this);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }
}
