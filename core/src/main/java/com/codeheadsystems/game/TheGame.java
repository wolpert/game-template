package com.codeheadsystems.game;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.di.DaggerGameComponent;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import com.codeheadsystems.game.ecs.component.VelocityComponent;
import javax.inject.Inject;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TheGame extends ApplicationAdapter {

    private static final String PLAYER_FLYING = "player1_Flying";
    private static final float PLAYER_FRAME_DURATION = 0.1f;
    private static final float PLAYER_BOTTOM_MARGIN = 0.05f;

    @Inject SpriteBatch batch;
    @Inject Texture image;
    @Inject TextureAtlas atlas;
    @Inject Engine engine;
    @Inject GameConfig config;

    @Override
    public void create() {
        // Build the Dagger graph here — providers may touch GL/Gdx, which is only valid after create().
        DaggerGameComponent.create().inject(this);

        engine.addEntity(buildBackground());
        engine.addEntity(buildPlayer());
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
        atlas.dispose();
    }

    private Entity buildBackground() {
        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.x = config.logo.x;
        pos.y = config.logo.y;
        pos.z = 0;
        TextureComponent tex = new TextureComponent();
        tex.region = new TextureRegion(image);
        entity.add(pos);
        entity.add(tex);
        return entity;
    }

    private Entity buildPlayer() {
        Array<TextureRegion> frames = new Array<>(atlas.findRegions(PLAYER_FLYING));
        if (frames.size == 0) {
            throw new IllegalStateException("Atlas is missing region: " + PLAYER_FLYING);
        }
        TextureRegion firstFrame = frames.first();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float x = (screenW - firstFrame.getRegionWidth()) / 2f;
        float y = screenH * PLAYER_BOTTOM_MARGIN;

        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.x = x;
        pos.y = y;
        pos.z = 1;
        TextureComponent tex = new TextureComponent();
        tex.region = firstFrame;
        AnimationComponent anim = new AnimationComponent();
        anim.animation = new Animation<>(PLAYER_FRAME_DURATION, frames, Animation.PlayMode.LOOP);
        entity.add(pos);
        entity.add(tex);
        entity.add(anim);
        entity.add(new VelocityComponent());
        entity.add(new InputComponent());
        return entity;
    }
}
