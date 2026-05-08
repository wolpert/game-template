package com.codeheadsystems.game.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PlayerComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import com.codeheadsystems.game.ecs.system.BlockSpawnSystem;
import com.codeheadsystems.game.session.GameState;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * The playable demo: input-driven kinematic player dodging dynamic blocks spawned by
 * {@link BlockSpawnSystem}. Each {@link #show()} starts a fresh session — entities cleared,
 * world bodies destroyed, score reset — so re-entering after game over restarts cleanly.
 */
@Singleton
public class GameScreen extends BaseScreen {

    private static final String PLAYER_FLYING = "player1_Flying";
    private static final float PLAYER_FRAME_DURATION = 0.1f;
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
    private final GameState state;
    private final BlockSpawnSystem spawner;
    private final Provider<ScreenNavigator> nav;

    private final Label scoreLabel;

    @Inject
    public GameScreen(Engine engine,
                      Texture image,
                      TextureAtlas atlas,
                      World world,
                      GameConfig config,
                      GameState state,
                      BlockSpawnSystem spawner,
                      Provider<ScreenNavigator> nav,
                      Skin skin) {
        this.engine = engine;
        this.image = image;
        this.atlas = atlas;
        this.world = world;
        this.config = config;
        this.state = state;
        this.spawner = spawner;
        this.nav = nav;

        scoreLabel = new Label("", skin);
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().left().pad(10);
        hud.add(scoreLabel);
        stage.addActor(hud);
    }

    @Override
    public void show() {
        // We don't want the stage to consume pointer input — InputSystem polls Gdx.input directly.
        Gdx.input.setInputProcessor(null);
        startNewSession();
    }

    private void startNewSession() {
        // Tear down any prior session: entities first (so removal listeners run), then bodies.
        engine.removeAllEntities();
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            world.destroyBody(b);
        }

        state.reset();
        spawner.reset();

        createGroundBody();
        engine.addEntity(buildBackground());
        engine.addEntity(buildPlayer());
    }

    @Override
    public void render(float delta) {
        if (state.isPlaying()) {
            state.elapsedSec += delta;
        }
        scoreLabel.setText(String.format("HP: %d   Time: %.1f", Math.max(0, state.hp), state.elapsedSec));

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        engine.update(delta);
        stage.act(delta);
        stage.draw();

        if (state.isGameOver()) {
            nav.get().goToGameOver();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            nav.get().goToMainMenu();
        }
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

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.KinematicBody;
        def.position.set((xPx + spriteW / 2f) / ppm, (yPx + spriteH / 2f) / ppm);
        Body body = world.createBody(def);

        PolygonShape shape = new PolygonShape();
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
        entity.add(new PlayerComponent());
        body.setUserData(entity);
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
