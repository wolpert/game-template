package com.codeheadsystems.game.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
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
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * The playable demo: background, input-driven kinematic player, falling block. Implements
 * {@link Screen} directly (not {@link BaseScreen}) because gameplay is driven by Ashley/Box2D,
 * not Scene2D — Scene2D is reserved for menu screens here.
 */
@Singleton
public class GameScreen implements Screen {

    private static final String PLAYER_FLYING = "player1_Flying";
    private static final String BLOCK_REGION = "block_block";
    private static final float PLAYER_FRAME_DURATION = 0.1f;
    private static final float BLOCK_FRAME_DURATION = 0.12f;
    private static final float PLAYER_BOTTOM_MARGIN = 0.05f;
    // Visible content of the player1_Flying sprite — there's transparent padding on each side.
    // Derive new values via: identify -format "%[bounding-box]" build/aseprite-frames/<frame>.png
    private static final int PLAYER_BODY_WIDTH = 64;
    private static final int PLAYER_BODY_HEIGHT = 116;

    private final Engine engine;
    private final Texture image;
    private final TextureAtlas atlas;
    private final World world;
    private final GameConfig config;
    private final Provider<ScreenNavigator> nav;

    private boolean entitiesBuilt;

    @Inject
    public GameScreen(Engine engine,
                      Texture image,
                      TextureAtlas atlas,
                      World world,
                      GameConfig config,
                      Provider<ScreenNavigator> nav) {
        this.engine = engine;
        this.image = image;
        this.atlas = atlas;
        this.world = world;
        this.config = config;
        this.nav = nav;
    }

    @Override
    public void show() {
        // No Scene2D stage for this screen — InputSystem polls Gdx.input directly, and ESC handling
        // is done via isKeyJustPressed in render(). Releasing the input processor matters because the
        // previous screen (LevelPicker) installed its stage as the processor.
        Gdx.input.setInputProcessor(null);
        if (!entitiesBuilt) {
            createGroundBody();
            engine.addEntity(buildBackground());
            engine.addEntity(buildPlayer());
            engine.addEntity(buildFallingBlock());
            entitiesBuilt = true;
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        engine.update(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            nav.get().goToMainMenu();
        }
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {}

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
        def.position.set((screenW / 2f) / ppm, (screenH * 0.85f) / ppm);
        Body body = world.createBody(def);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox((blockW / 2f) / ppm, (blockH / 2f) / ppm);
        FixtureDef fixture = new FixtureDef();
        fixture.shape = shape;
        fixture.density = 1f;
        fixture.friction = 0.3f;
        fixture.restitution = 0.5f;
        body.createFixture(fixture);
        shape.dispose();

        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.z = 2;
        entity.add(pos);
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
        edge.set(0f, 0f, screenWidthMeters, 0f);
        ground.createFixture(edge, 0f);
        edge.dispose();
    }
}
