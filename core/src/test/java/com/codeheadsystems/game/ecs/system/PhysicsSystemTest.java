package com.codeheadsystems.game.ecs.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PhysicsSystemTest {

    private static final float PPM = 32f;

    private World world;
    private Engine engine;

    @BeforeAll
    static void loadNatives() {
        // gdx-platform + gdx-box2d-platform natives are on the test classpath; both calls are idempotent.
        GdxNativesLoader.load();
        Box2D.init();
    }

    @BeforeEach
    void setUp() {
        GameConfig config = new GameConfig();
        config.physics = new GameConfig.PhysicsConfig();
        config.physics.pixelsPerMeter = PPM;
        // Zero gravity so kinematic motion in these tests is purely from linearVelocity.
        world = new World(new Vector2(0f, 0f), true);
        engine = new PooledEngine();
        engine.addSystem(new PhysicsSystem(world, config));
    }

    @AfterEach
    void tearDown() {
        world.dispose();
    }

    @Test
    void doesNotAdvanceBelowFixedTimestep() {
        Body body = bodyWithVelocity(0f, 0f, 10f, 0f);

        engine.update(0.005f); // less than 1/60

        assertEquals(0f, body.getPosition().x, 1e-6, "no step should have run");
    }

    @Test
    void advancesOnceWhenAccumulatorReachesFixedTimestep() {
        Body body = bodyWithVelocity(0f, 0f, 10f, 0f);

        engine.update(0.02f); // > 1/60

        // After one fixed step at 10 m/s: ~0.1667 m
        assertEquals(10f * PhysicsSystem.STEP_SECONDS, body.getPosition().x, 1e-4);
    }

    @Test
    void cumulativeDeltasTriggerASingleStep() {
        Body body = bodyWithVelocity(0f, 0f, 10f, 0f);

        engine.update(0.005f);
        engine.update(0.005f);
        engine.update(0.005f);
        assertEquals(0f, body.getPosition().x, 1e-6, "still under threshold across 3 ticks");

        engine.update(0.005f); // accumulator now 0.020 -> one step

        assertEquals(10f * PhysicsSystem.STEP_SECONDS, body.getPosition().x, 1e-4);
    }

    @Test
    void cappedFrameTimePreventsRunawayAccumulation() {
        Body body = bodyWithVelocity(0f, 0f, 1f, 0f);

        // A 10s "pause" must not advance the body 10 meters — accumulator caps the input delta to 0.25s.
        engine.update(10f);

        assertTrue(body.getPosition().x <= 0.25f + 1e-3,
                "expected <= 0.25m of motion, got " + body.getPosition().x);
    }

    @Test
    void syncsBodyCenterToTextureBottomLeftInPixels() {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.KinematicBody;
        def.position.set(2f, 3f);
        Body body = world.createBody(def);

        BodyComponent bc = new BodyComponent();
        bc.body = body;
        PositionComponent pos = new PositionComponent();
        TextureComponent tex = new TextureComponent();
        TextureRegion region = mock(TextureRegion.class);
        when(region.getRegionWidth()).thenReturn(32);
        when(region.getRegionHeight()).thenReturn(64);
        tex.region = region;

        Entity e = new Entity();
        e.add(bc);
        e.add(pos);
        e.add(tex);
        engine.addEntity(e);

        engine.update(0.02f);

        // body center at (2m, 3m) * 32 ppm = (64px, 96px); minus half-extents (16, 32) -> (48, 64).
        assertEquals(48f, pos.x, 1e-4);
        assertEquals(64f, pos.y, 1e-4);
    }

    private Body bodyWithVelocity(float x, float y, float vx, float vy) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.KinematicBody;
        def.position.set(x, y);
        def.linearVelocity.set(vx, vy);
        return world.createBody(def);
    }
}
