package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Common Scene2D scaffolding for menu-style screens. Subclasses build their UI in their constructor
 * (the {@link Stage} is allocated in the {@code @Inject} ctor of this base class), and may override
 * {@link #render(float)} for custom drawing on top of the stage.
 */
public abstract class BaseScreen implements Screen {

    protected final Stage stage = new Stage(new ScreenViewport());

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.10f, 0.10f, 0.15f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        // Don't dispose the stage here — screens are @Singleton and may be shown again.
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
