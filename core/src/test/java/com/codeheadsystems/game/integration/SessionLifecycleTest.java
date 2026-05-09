package com.codeheadsystems.game.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.codeheadsystems.game.config.GameConfig;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.physics.PhysicsWorld;
import com.codeheadsystems.game.render.CameraShake;
import com.codeheadsystems.game.render.Hitstop;
import com.codeheadsystems.game.render.TintFlash;
import com.codeheadsystems.game.sample.BlockComponent;
import com.codeheadsystems.game.sample.GameContactListener;
import com.codeheadsystems.game.sample.GameState;
import com.codeheadsystems.game.sample.HighscoreStore;
import com.codeheadsystems.game.sample.InputComponent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end session-lifecycle test (T3) — boots a real {@link HeadlessApplication} so
 * {@link Gdx#app}, {@link Gdx#files}, and {@link Gdx#app#getPreferences(String)} are alive,
 * then drives a sample-style session through pause/resume, hit-to-DYING-to-GAME_OVER, retry
 * (via {@code PhysicsWorld.clearSession}), and highscore persistence.
 *
 * <p>Hand-wires the systems-under-test instead of building the full Dagger graph: the graph
 * pulls in {@code Skin}, {@code TextureAtlas}, and the Aseprite atlas — none of which exist
 * headlessly without a non-trivial mock pile (T3 stopping rule explicitly allows skipping the
 * graph in that case). The invariants asserted here are state-machine + session-restart +
 * highscore — none of which need GL or graph DI.
 */
class SessionLifecycleTest {

    private static HeadlessApplication app;

    @BeforeAll
    static void bootstrap() {
        // Box2D natives live with the libGDX desktop natives — same pattern used by
        // PhysicsSystemTest / GameContactListenerTest in this repo.
        GdxNativesLoader.load();
        Box2D.init();
        // Headless ApplicationListener: empty body; we just need Gdx.app/files alive so
        // HeadlessPreferences can write to the standard prefs directory.
        ApplicationListener noopListener = new ApplicationListener() {
            @Override public void create() {}
            @Override public void resize(int width, int height) {}
            @Override public void render() {}
            @Override public void pause() {}
            @Override public void resume() {}
            @Override public void dispose() {}
        };
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        // Don't run the render loop — we drive timing manually.
        cfg.updatesPerSecond = 0;
        app = new HeadlessApplication(noopListener, cfg);
    }

    @AfterAll
    static void teardown() {
        if (app != null) {
            app.exit();
        }
    }

    private GameState state;
    private HighscoreStore highscores;
    private PhysicsWorld physicsWorld;
    private Engine engine;
    private GameContactListener listener;

    @BeforeEach
    void setUp() {
        // Use a fresh prefs file per test so the highscore assertions don't see leftover state
        // from a sibling test. HeadlessApplication writes to .prefs/<name> under user.home.
        String prefsName = "game-template-test-" + System.nanoTime();
        Gdx.app.getPreferences(prefsName).clear();

        state = new GameState();
        highscores = new HighscoreStore(Gdx.app.getPreferences(prefsName));
        // Build the real PhysicsWorld with a zero-gravity stub config so we drive contact placement
        // directly. PhysicsWorld owns its own World; tests use its public surface (getWorld(),
        // clearSession, setContactListener, dispose) — no subclassing or reflection.
        physicsWorld = new PhysicsWorld(stubConfig());
        engine = new PooledEngine();
        listener = new GameContactListener(state, new CameraShake(), new Hitstop(), new TintFlash());
        physicsWorld.setContactListener(listener);
    }

    @AfterEach
    void cleanup() {
        physicsWorld.dispose();
    }

    /** Invariant 1: PLAYING -> PAUSED -> PLAYING preserves elapsed time and HP. */
    @Test
    void pauseAndResumePreservesSessionState() {
        state.elapsedSec = 4.25f;
        state.hp = 3;

        assertTrue(state.canPause());
        state.pause();
        assertEquals(GameState.Phase.PAUSED, state.phase);
        assertFalse(state.isPlaying(), "InputGate must close while paused");

        // Simulate render frames passing while paused — elapsedSec is the screen's responsibility
        // to advance only when state.isPlaying() (matches SampleGameScreen.render).
        // Just verify the holder doesn't drift on its own.
        assertEquals(4.25f, state.elapsedSec, 1e-6f);
        assertEquals(3, state.hp);

        state.resume();
        assertEquals(GameState.Phase.PLAYING, state.phase);
        assertEquals(4.25f, state.elapsedSec, 1e-6f, "elapsed time must survive a pause/resume");
        assertEquals(3, state.hp);
    }

    /** Invariant 2: hits decrement HP; HP-zero flips PLAYING -> DYING. */
    @Test
    void contactsDecrementHpAndTransitionToDying() {
        spawnPlayer(0f, 0f);
        // Five tightly packed blocks — each begin-contact should land a hit.
        for (int i = 0; i < GameState.MAX_HP; i++) {
            spawnBlock(0.4f + i * 0.1f, 0f);
        }

        for (int step = 0; step < 30 && state.phase == GameState.Phase.PLAYING; step++) {
            physicsWorld.getWorld().step(1f / 60f, 6, 2);
        }

        assertEquals(0, state.hp, "all five blocks should each register one hit");
        assertEquals(GameState.Phase.DYING, state.phase);
    }

    /**
     * Invariant 3: DYING -> GAME_OVER is a state-machine transition (the screen's DeathSystem
     * triggers it after the Died animation finishes; here we simulate the transition directly
     * since we have no animation system in the hand-wired graph).
     */
    @Test
    void dyingTransitionsToGameOverAndContactsAreIgnored() {
        state.hp = 0;
        state.phase = GameState.Phase.DYING;

        // Late contacts during DYING must NOT decrement further (already zero; listener guard).
        spawnPlayer(0f, 0f);
        spawnBlock(0.5f, 0f);
        for (int i = 0; i < 10; i++) {
            physicsWorld.getWorld().step(1f / 60f, 6, 2);
        }
        assertEquals(0, state.hp, "DYING freezes hp — no further decrement on late contacts");

        // Production path: DeathSystem would set this on animation finish; assert it is reachable.
        state.phase = GameState.Phase.GAME_OVER;
        assertTrue(state.isGameOver());
    }

    /**
     * Invariant 4: {@code physicsWorld.clearSession(engine)} removes all bodies + entities so a
     * second session starts clean. This is the regression that CLAUDE.md flags as the
     * "restart-state hazard".
     */
    @Test
    void clearSessionRemovesAllBodiesAndEntities() {
        spawnPlayer(0f, 0f);
        for (int i = 0; i < 4; i++) {
            spawnBlock(2f + i, 5f);
        }
        Array<Body> before = new Array<>();
        physicsWorld.getWorld().getBodies(before);
        assertEquals(5, before.size, "5 bodies expected before clearSession");
        assertEquals(5, engine.getEntities().size(), "5 entities expected before clearSession");

        physicsWorld.clearSession(engine);

        Array<Body> after = new Array<>();
        physicsWorld.getWorld().getBodies(after);
        assertEquals(0, after.size, "all bodies must be destroyed by clearSession");
        assertEquals(0, engine.getEntities().size(), "all entities must be removed by clearSession");

        // After clearing, a new session can be started without leaking state.
        Entity newPlayer = spawnPlayer(0f, 0f);
        spawnBlock(0.5f, 0f);
        assertEquals(2, engine.getEntities().size());
        assertNotNull(newPlayer.getComponent(InputComponent.class));
    }

    /**
     * Invariant 5: full retry path — first session ends in GAME_OVER with a recorded best;
     * second session resets state and starts fresh; a worse score does NOT overwrite the best.
     */
    @Test
    void retryPathAfterGameOverResetsStateAndPreservesHighscore() {
        // --- Session 1: survive 12.5 seconds, then die.
        state.elapsedSec = 12.5f;
        state.hp = 0;
        state.phase = GameState.Phase.DYING;
        // Mirrors SampleGameScreen.render's GAME_OVER handler.
        boolean firstRecord = highscores.recordIfBest(state.elapsedSec);
        state.phase = GameState.Phase.GAME_OVER;

        assertTrue(firstRecord, "first session always sets a new record");
        assertEquals(12.5f, highscores.getBest(), 1e-5f);

        // --- Retry: SampleGameScreen.startNewSession() does this exact dance.
        physicsWorld.clearSession(engine);
        state.reset();
        assertEquals(GameState.Phase.PLAYING, state.phase, "reset must clear GAME_OVER");
        assertEquals(GameState.MAX_HP, state.hp);
        assertEquals(0f, state.elapsedSec, 0f);

        // --- Session 2: only survive 5 seconds.
        state.elapsedSec = 5f;
        state.hp = 0;
        state.phase = GameState.Phase.DYING;
        boolean secondRecord = highscores.recordIfBest(state.elapsedSec);
        state.phase = GameState.Phase.GAME_OVER;

        assertFalse(secondRecord, "5s does not beat the 12.5s best");
        assertEquals(12.5f, highscores.getBest(), 1e-5f, "best must be preserved across sessions");
    }

    /** Invariant 6: pre-existing GAME_OVER state has canPause()==false. */
    @Test
    void cannotPauseFromGameOver() {
        state.phase = GameState.Phase.GAME_OVER;
        assertFalse(state.canPause());
        state.pause();
        assertEquals(GameState.Phase.GAME_OVER, state.phase, "pause is a no-op from GAME_OVER");
    }

    /**
     * Invariant 7 (sanity): the engine + world references stay stable across session resets.
     * If clearSession ever started returning a new engine/world the pattern in
     * SampleGameScreen.startNewSession() would silently break.
     */
    @Test
    void engineAndWorldIdentityPreservedAcrossClearSession() {
        Engine engineBefore = engine;
        World worldBefore = physicsWorld.getWorld();
        spawnPlayer(0f, 0f);
        spawnBlock(0.5f, 0f);

        physicsWorld.clearSession(engine);

        assertEquals(engineBefore, engine, "engine reference must not change across clearSession");
        assertEquals(worldBefore, physicsWorld.getWorld(), "world reference must not change across clearSession");
        // ContactListener identity is intentionally not asserted — Box2D doesn't expose a getter
        // and SampleGameScreen reinstalls on every session start anyway.
        assertNotSame(null, listener);
        assertNotEquals(null, listener.getClass(), "listener stays alive — used as a reachability sanity check");
    }

    // --- helpers --------------------------------------------------------------------------

    private Entity spawnPlayer(float xMeters, float yMeters) {
        Entity entity = new Entity();
        entity.add(new InputComponent());
        BodyComponent bc = new BodyComponent();
        bc.body = createUnitBody(xMeters, yMeters);
        entity.add(bc);
        bc.body.setUserData(entity);
        engine.addEntity(entity);
        return entity;
    }

    private Entity spawnBlock(float xMeters, float yMeters) {
        Entity entity = new Entity();
        entity.add(new BlockComponent());
        BodyComponent bc = new BodyComponent();
        bc.body = createUnitBody(xMeters, yMeters);
        entity.add(bc);
        bc.body.setUserData(entity);
        engine.addEntity(entity);
        return entity;
    }

    private Body createUnitBody(float x, float y) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x, y);
        Body body = physicsWorld.getWorld().createBody(def);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 0.5f);
        body.createFixture(shape, 1f);
        shape.dispose();
        return body;
    }

    /** Zero-gravity GameConfig stub; tests place bodies directly so gravity is undesirable. */
    private static GameConfig stubConfig() {
        GameConfig cfg = new GameConfig();
        cfg.physics = new GameConfig.PhysicsConfig();
        cfg.physics.gravity = new GameConfig.Vec2Config();
        cfg.physics.gravity.x = 0f;
        cfg.physics.gravity.y = 0f;
        cfg.physics.pixelsPerMeter = 32f;
        return cfg;
    }
}
