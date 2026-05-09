package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.debug.DebugOverlay;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import com.codeheadsystems.game.ecs.component.VelocityComponent;
import com.codeheadsystems.game.ecs.component.WrapAroundComponent;
import com.codeheadsystems.game.flow.SessionResult;
import com.codeheadsystems.game.lifecycle.LifecycleGate;
import com.codeheadsystems.game.physics.PhysicsWorld;
import com.codeheadsystems.game.render.Hitstop;
import com.codeheadsystems.game.screens.ScreenNavigator;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * The dodge demo's playable screen — input-driven kinematic player evading dynamic blocks spawned
 * by {@link BlockSpawnSystem}. Implements {@link Screen} directly (not via {@code BaseScreen})
 * because gameplay is Ashley/Box2D-driven, not Scene2D — the small Scene2D {@link Stage} here is
 * reserved for the HUD and the pause overlay.
 *
 * <p>Each {@link #show()} starts a fresh session — entities cleared, world bodies destroyed, score
 * reset — so re-entering after game over restarts cleanly. The pause state lives on
 * {@link GameState}; while paused, {@code engine.update} receives a delta of zero so Box2D's
 * accumulator never advances, and a Scene2D overlay is drawn over the last frame.
 */
@Singleton
public class SampleGameScreen implements Screen {

    private static final String PLAYER_FLYING = "player1_Flying";
    private static final float PLAYER_FRAME_DURATION = 0.1f;
    private static final float PLAYER_BOTTOM_MARGIN = 0.05f;
    // Visible content of the player1_Flying sprite — there's transparent padding on each side.
    // Derive new values via: identify -format "%[bounding-box]" build/aseprite-frames/<frame>.png
    private static final int PLAYER_BODY_WIDTH = 64;
    private static final int PLAYER_BODY_HEIGHT = 116;
    /** Background drift in px/s — slow enough to read as parallax, not motion sickness. */
    private static final float BACKGROUND_DRIFT_PX_PER_SEC = 30f;

    private final Engine engine;
    private final Texture logoTexture;
    private final TextureAtlas atlas;
    private final PhysicsWorld physicsWorld;
    private final GameConfig config;
    private final GameState state;
    private final BlockSpawnSystem spawner;
    private final Provider<ScreenNavigator> nav;
    private final DebugOverlay debugOverlay;
    private final SessionResult result;
    private final GameContactListener contactListener;
    private final HighscoreStore highscores;
    private final Hitstop hitstop;
    private final Skin skin;
    private final LifecycleGate lifecycleGate;

    /** HUD + pause overlay both live on this Stage. The pause overlay starts hidden. */
    private final Stage stage;
    private final Label scoreLabel;
    private final Container<Table> pauseOverlay;
    /** Top-right pause button — visible during play, hidden while paused. */
    private final Container<TextButton> pauseButtonContainer;

    /** True after we've populated SessionResult once for this game-over so we don't double-record. */
    private boolean gameOverHandled;

    // HUD allocation skip: rebuild the label text only when the displayed values actually change.
    // Time is rendered to one decimal, so we snapshot tenths-of-a-second — most frames change
    // neither hp nor tenths and we skip setText entirely.
    private final StringBuilder hudBuilder = new StringBuilder(32);
    private int lastHp = -1;
    private int lastTimeTenths = -1;

    private final InputAdapter keyAdapter = new InputAdapter() {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                if (state.isPaused()) {
                    resumeGame();
                } else if (state.canPause()) {
                    pauseGame();
                } else {
                    // GAME_OVER / DYING — fall back to leaving the demo entirely.
                    nav.get().goToMainMenu();
                }
                return true;
            }
            return false;
        }
    };

    @Inject
    public SampleGameScreen(Engine engine,
                            Texture logoTexture,
                            TextureAtlas atlas,
                            PhysicsWorld physicsWorld,
                            GameConfig config,
                            GameState state,
                            BlockSpawnSystem spawner,
                            Provider<ScreenNavigator> nav,
                            DebugOverlay debugOverlay,
                            SessionResult result,
                            GameContactListener contactListener,
                            HighscoreStore highscores,
                            Hitstop hitstop,
                            Skin skin,
                            LifecycleGate lifecycleGate) {
        this.engine = engine;
        this.logoTexture = logoTexture;
        this.atlas = atlas;
        this.physicsWorld = physicsWorld;
        this.config = config;
        this.state = state;
        this.spawner = spawner;
        this.nav = nav;
        this.debugOverlay = debugOverlay;
        this.result = result;
        this.contactListener = contactListener;
        this.highscores = highscores;
        this.hitstop = hitstop;
        this.skin = skin;
        this.lifecycleGate = lifecycleGate;

        this.stage = new Stage(new ScreenViewport());

        scoreLabel = new Label("", skin);
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().left().pad(10);
        hud.add(scoreLabel);
        stage.addActor(hud);

        // Pause button — top-right; small enough not to crowd the HUD, big enough for touch.
        TextButton pauseBtn = new TextButton("Pause", skin);
        pauseBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (state.isPaused()) resumeGame();
                else if (state.canPause()) pauseGame();
            }
        });
        pauseButtonContainer = new Container<>(pauseBtn);
        pauseButtonContainer.setFillParent(true);
        pauseButtonContainer.top().right().pad(10);
        pauseButtonContainer.minWidth(96).minHeight(48);
        stage.addActor(pauseButtonContainer);

        pauseOverlay = buildPauseOverlay();
        pauseOverlay.setVisible(false);
        stage.addActor(pauseOverlay);
    }

    /**
     * Pause overlay: a translucent backdrop + Resume / Quit-to-menu buttons. Construction
     * happens once and is toggled by {@link #pauseGame()} / {@link #resumeGame()}.
     */
    private Container<Table> buildPauseOverlay() {
        Table panel = new Table();
        panel.defaults().pad(6).width(220).height(48);
        panel.add(new Label("Paused", skin)).padBottom(20).row();

        TextButton resume = new TextButton("Resume", skin);
        resume.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                resumeGame();
            }
        });
        panel.add(resume).row();

        TextButton quit = new TextButton("Quit to Menu", skin);
        quit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.get().goToMainMenu();
            }
        });
        panel.add(quit).row();

        Container<Table> wrap = new Container<>(panel);
        wrap.setFillParent(true);
        wrap.center();
        // Skin's "default" drawable is reused as a translucent backdrop — close enough without
        // shipping a dedicated nine-patch in the atlas.
        return wrap;
    }

    @Override
    public void show() {
        // Stage takes input first (so pause-button + overlay buttons receive clicks), then the
        // key adapter handles ESC/BACK toggling. InputSystem polls Gdx.input directly, so it
        // doesn't need to be in this multiplexer.
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        InputMultiplexer mux = new InputMultiplexer();
        mux.addProcessor(stage);
        mux.addProcessor(keyAdapter);
        Gdx.input.setInputProcessor(mux);
        startNewSession();
    }

    private void startNewSession() {
        // Single-call session reset — PhysicsWorld owns the bodies+entities ordering so we don't
        // duplicate the dance here.
        physicsWorld.clearSession(engine);

        state.reset();
        spawner.reset();
        gameOverHandled = false;
        // Force the next render to push fresh HUD text after session reset.
        lastHp = -1;
        lastTimeTenths = -1;
        pauseOverlay.setVisible(false);
        pauseButtonContainer.setVisible(true);

        // Install the demo's contact listener on every entry — idempotent, and keeps
        // the World provider scaffold-pure (no demo-specific knowledge).
        physicsWorld.setContactListener(contactListener);

        createGroundBody();
        engine.addEntity(buildBackground());
        engine.addEntity(buildPlayer());
    }

    /**
     * Allocation-free HUD update: reuses {@link #hudBuilder} and only calls {@link Label#setText}
     * when the displayed integer/tenths values change. {@code Label.setText(CharSequence)} copies
     * to its own glyph buffer, so passing the reused {@code StringBuilder} avoids both the
     * {@code String.format} string and the {@code Float.toString} allocations the previous form
     * incurred every frame.
     */
    private void updateHudLabel() {
        int displayHp = Math.max(0, state.hp);
        int currentTenths = (int) (state.elapsedSec * 10);
        if (displayHp == lastHp && currentTenths == lastTimeTenths) {
            return;
        }
        lastHp = displayHp;
        lastTimeTenths = currentTenths;
        hudBuilder.setLength(0);
        hudBuilder.append("HP: ").append(displayHp)
                .append("   Time: ").append(currentTenths / 10).append('.').append(currentTenths % 10);
        scoreLabel.setText(hudBuilder);
    }

    private void pauseGame() {
        if (!state.canPause()) return;
        state.pause();
        pauseOverlay.setVisible(true);
        pauseButtonContainer.setVisible(false);
    }

    private void resumeGame() {
        if (!state.isPaused()) return;
        state.resume();
        pauseOverlay.setVisible(false);
        pauseButtonContainer.setVisible(true);
    }

    @Override
    public void render(float delta) {
        // Wall-clock time always advances the hitstop timer so a freeze always lifts after its
        // requested duration regardless of frame rate.
        hitstop.tick(delta);

        if (state.isPlaying()) {
            state.elapsedSec += delta;
        }
        updateHudLabel();

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Pause halts the ECS; hitstop scales it temporarily; app backgrounding zeroes it. Any
        // path that yields zero also resets PhysicsSystem's accumulator so a long pause doesn't
        // catch up by ticking many simulated steps on resume.
        float scaledDelta = (state.isPaused() || !lifecycleGate.isAppActive())
                ? 0f
                : delta * hitstop.getEngineDeltaScale();
        engine.update(scaledDelta);
        stage.act(delta);
        stage.draw();
        debugOverlay.render(delta);

        if (state.isGameOver() && !gameOverHandled) {
            gameOverHandled = true;
            boolean isNew = highscores.recordIfBest(state.elapsedSec);
            result.headline = "Game Over";
            result.detail = String.format("Time survived: %.1f s", state.elapsedSec);
            result.retryAvailable = true;
            result.onRetry = () -> nav.get().goToSampleGame();
            result.bestSec = highscores.getBest();
            result.newRecord = isNew;
            nav.get().goToGameOver();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        debugOverlay.resize(width, height);
    }

    @Override
    public void pause() {
        // App backgrounding (Android) — auto-pause the gameplay so the player doesn't return to
        // an ongoing session that's been frozen at the JVM level. Idempotent.
        if (state.canPause()) {
            pauseGame();
        }
    }

    @Override
    public void resume() {
        // Don't auto-resume on app foreground — a stale return shouldn't toss the user back into
        // a hot session. The pause overlay stays up, user clicks Resume.
    }

    @Override
    public void hide() {
        // Keep this @Singleton screen's state intact — show() will rebuild on next entry.
    }

    @Override
    public void dispose() {
        stage.dispose();
        debugOverlay.dispose();
    }

    /**
     * Demo consumer of {@link com.codeheadsystems.game.ecs.system.MovementSystem}: the libGDX
     * logo drifts horizontally at a slow constant speed and loops via
     * {@link com.codeheadsystems.game.ecs.system.WrapAroundSystem}. Pure cosmetic motion — no
     * Box2D body, so no collision.
     */
    private Entity buildBackground() {
        Entity entity = engine.createEntity();
        PositionComponent pos = engine.createComponent(PositionComponent.class);
        pos.x = config.logo.x;
        pos.y = config.logo.y;
        pos.z = 0;
        TextureComponent tex = engine.createComponent(TextureComponent.class);
        tex.region = new TextureRegion(logoTexture);
        VelocityComponent vel = engine.createComponent(VelocityComponent.class);
        vel.dx = BACKGROUND_DRIFT_PX_PER_SEC;
        WrapAroundComponent wrap = engine.createComponent(WrapAroundComponent.class);
        wrap.widthPx = logoTexture.getWidth();
        entity.add(pos);
        entity.add(tex);
        entity.add(vel);
        entity.add(wrap);
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

        // Player is built once per session, not per spawn — local instances are fine here.
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.KinematicBody;
        def.position.set((xPx + spriteW / 2f) / ppm, (yPx + spriteH / 2f) / ppm);
        Body body = physicsWorld.getWorld().createBody(def);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox((PLAYER_BODY_WIDTH / 2f) / ppm, (PLAYER_BODY_HEIGHT / 2f) / ppm);
        body.createFixture(shape, 0f);
        shape.dispose();

        Entity entity = engine.createEntity();
        PositionComponent pos = engine.createComponent(PositionComponent.class);
        pos.x = xPx;
        pos.y = yPx;
        pos.z = 1;
        TextureComponent tex = engine.createComponent(TextureComponent.class);
        tex.region = firstFrame;
        AnimationComponent anim = engine.createComponent(AnimationComponent.class);
        anim.animation = new Animation<>(PLAYER_FRAME_DURATION, frames, Animation.PlayMode.LOOP);
        BodyComponent bc = engine.createComponent(BodyComponent.class);
        bc.body = body;
        entity.add(pos);
        entity.add(tex);
        entity.add(anim);
        entity.add(bc);
        entity.add(engine.createComponent(InputComponent.class));
        entity.add(engine.createComponent(PlayerComponent.class));
        body.setUserData(entity);
        return entity;
    }

    private void createGroundBody() {
        float ppm = config.physics.pixelsPerMeter;
        float screenWidthMeters = Gdx.graphics.getWidth() / ppm;

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        Body ground = physicsWorld.getWorld().createBody(def);

        EdgeShape edge = new EdgeShape();
        edge.set(0f, 0f, screenWidthMeters, 0f);
        ground.createFixture(edge, 0f);
        edge.dispose();
    }

}
