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
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.di.DaggerGameComponent;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class TheGame extends ApplicationAdapter {

    private static final String PLAYER_FLYING = "player1_Flying";
    private static final String BLOCK_REGION = "block_block";
    private static final float PLAYER_FRAME_DURATION = 0.1f;
    private static final float BLOCK_FRAME_DURATION = 0.12f;
    private static final float PLAYER_BOTTOM_MARGIN = 0.05f;
    // Visible content of the player1_Flying sprite — there's transparent padding on each side.
    // Derive new values via: identify -format "%[bounding-box]" build/aseprite-frames/<frame>.png
    private static final int PLAYER_BODY_WIDTH = 64;
    private static final int PLAYER_BODY_HEIGHT = 116;

    @Inject SpriteBatch batch;
    @Inject Texture image;
    @Inject TextureAtlas atlas;
    @Inject Engine engine;
    @Inject GameConfig config;
    @Inject World world;

    @Override
    public void create() {
        // Build the Dagger graph here — providers may touch GL/Gdx, which is only valid after create().
        DaggerGameComponent.create().inject(this);

        createGroundBody();
        engine.addEntity(buildBackground());
        engine.addEntity(buildPlayer());
        engine.addEntity(buildFallingBlock());
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
        world.dispose();
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
        int spriteW = firstFrame.getRegionWidth();
        int spriteH = firstFrame.getRegionHeight();

        float ppm = config.physics.pixelsPerMeter;
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float xPx = (screenW - spriteW) / 2f;
        float yPx = screenH * PLAYER_BOTTOM_MARGIN;

        // Kinematic so input drives linear velocity directly; collisions push dynamic bodies
        // (e.g. the falling block) but the player itself is not pushed back.
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.KinematicBody;
        def.position.set((xPx + spriteW / 2f) / ppm, (yPx + spriteH / 2f) / ppm);
        Body body = world.createBody(def);

        PolygonShape shape = new PolygonShape();
        // Body matches the visible character, not the full padded frame.
        shape.setAsBox((PLAYER_BODY_WIDTH / 2f) / ppm, (PLAYER_BODY_HEIGHT / 2f) / ppm);
        body.createFixture(shape, 0f);
        shape.dispose();

        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.x = xPx;
        pos.y = yPx;
        pos.z = 1;
        TextureComponent tex = new TextureComponent();
        tex.region = firstFrame;
        AnimationComponent anim = new AnimationComponent();
        anim.animation = new Animation<>(PLAYER_FRAME_DURATION, frames, Animation.PlayMode.LOOP);
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(pos);
        entity.add(tex);
        entity.add(anim);
        entity.add(bc);
        entity.add(new InputComponent());
        return entity;
    }

    private Entity buildFallingBlock() {
        Array<TextureRegion> frames = new Array<>(atlas.findRegions(BLOCK_REGION));
        if (frames.size == 0) {
            throw new IllegalStateException("Atlas is missing region: " + BLOCK_REGION);
        }
        TextureRegion firstFrame = frames.first();
        int blockW = firstFrame.getRegionWidth();
        int blockH = firstFrame.getRegionHeight();

        float ppm = config.physics.pixelsPerMeter;
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        // Spawn near the top of the screen, horizontally centered.
        def.position.set((screenW / 2f) / ppm, (screenH * 0.85f) / ppm);
        Body body = world.createBody(def);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox((blockW / 2f) / ppm, (blockH / 2f) / ppm);
        FixtureDef fixture = new FixtureDef();
        fixture.shape = shape;
        fixture.density = 1f;
        fixture.friction = 0.3f;
        fixture.restitution = 0.5f; // bouncy enough to be visibly alive
        body.createFixture(fixture);
        shape.dispose();

        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.z = 2;
        entity.add(pos); // PhysicsSystem fills x/y each tick.
        TextureComponent tex = new TextureComponent();
        tex.region = firstFrame;
        entity.add(tex);
        AnimationComponent anim = new AnimationComponent();
        anim.animation = new Animation<>(BLOCK_FRAME_DURATION, frames, Animation.PlayMode.LOOP);
        entity.add(anim);
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        return entity;
    }

    private void createGroundBody() {
        float ppm = config.physics.pixelsPerMeter;
        float screenWidthMeters = Gdx.graphics.getWidth() / ppm;

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        Body ground = world.createBody(def);

        EdgeShape edge = new EdgeShape();
        // Flat ground at y = 0 spanning the screen.
        edge.set(0f, 0f, screenWidthMeters, 0f);
        ground.createFixture(edge, 0f);
        edge.dispose();
    }
}
