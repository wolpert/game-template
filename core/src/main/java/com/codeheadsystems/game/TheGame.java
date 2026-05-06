package com.codeheadsystems.game;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codeheadsystems.game.di.DaggerGameComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TheGame extends ApplicationAdapter {
    @Inject SpriteBatch batch;
    @Inject Texture image;
    @Inject Engine engine;

    @Override
    public void create() {
        // Build the Dagger graph here — providers may touch GL/Gdx, which is only valid after create().
        DaggerGameComponent.create().inject(this);

        Entity logo = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.x = 140;
        pos.y = 210;
        TextureComponent tex = new TextureComponent();
        tex.texture = image;
        logo.add(pos);
        logo.add(tex);
        engine.addEntity(logo);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        engine.update(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }
}
