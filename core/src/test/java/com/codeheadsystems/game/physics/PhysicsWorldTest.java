package com.codeheadsystems.game.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.codeheadsystems.game.config.GameConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Real-Box2D test of {@link PhysicsWorld} — Mockito can't instrument the JNI-backed
 * {@code World}/{@code Body} types, so we run actual native code (same pattern as
 * {@code PhysicsSystemTest}).
 */
class PhysicsWorldTest {

    private PhysicsWorld physicsWorld;
    private Engine engine;

    @BeforeAll
    static void loadNatives() {
        // gdx-platform + gdx-box2d-platform natives are on the test classpath; idempotent.
        GdxNativesLoader.load();
        Box2D.init();
    }

    @BeforeEach
    void setUp() {
        GameConfig config = new GameConfig();
        config.physics = new GameConfig.PhysicsConfig();
        config.physics.pixelsPerMeter = 32f;
        config.physics.gravity = new GameConfig.Vec2Config();
        config.physics.gravity.x = 0f;
        config.physics.gravity.y = 0f;
        physicsWorld = new PhysicsWorld(config);
        engine = new PooledEngine();
    }

    @AfterEach
    void tearDown() {
        physicsWorld.dispose();
    }

    @Test
    void getWorldReturnsNonNullBox2dWorld() {
        World world = physicsWorld.getWorld();
        assertNotNull(world);
    }

    @Test
    void setContactListenerInstallsListenerOnUnderlyingWorld() {
        ContactListener listener = mock(ContactListener.class);
        // No throw and no observable side effect we can assert against directly without colliding
        // bodies — the value of this test is that the wrapper accepts the listener without NPE.
        physicsWorld.setContactListener(listener);
        // Replacing it must also be fine (idempotent install pattern that SampleGameScreen relies on).
        physicsWorld.setContactListener(listener);
    }

    @Test
    void clearSessionDestroysAllBodiesAndRemovesAllEntities() {
        World world = physicsWorld.getWorld();
        // Set up a session with bodies + matching ECS entities.
        Body bodyA = createBody(world);
        Body bodyB = createBody(world);
        Entity entityA = new Entity();
        Entity entityB = new Entity();
        bodyA.setUserData(entityA);
        bodyB.setUserData(entityB);
        engine.addEntity(entityA);
        engine.addEntity(entityB);

        assertEquals(2, engine.getEntities().size());
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        assertEquals(2, bodies.size);

        physicsWorld.clearSession(engine);

        // Both sides cleared.
        bodies.clear();
        world.getBodies(bodies);
        assertEquals(0, bodies.size, "all bodies should be destroyed");
        assertEquals(0, engine.getEntities().size(), "all entities should be removed");
    }

    @Test
    void clearSessionRunsBothHalvesInOneCall() {
        // The ordering contract is internal to PhysicsWorld#clearSession (bodies first, then
        // entities) and not directly observable from outside without a destruction-listener
        // probe — but the externally-visible postcondition is: after one call, both the world
        // body list AND the engine entity set are empty. That's the cohesion guarantee
        // SampleGameScreen relies on, and the bug fix being tested.
        World world = physicsWorld.getWorld();
        Body body = createBody(world);
        Entity entity = new Entity();
        body.setUserData(entity);
        engine.addEntity(entity);

        physicsWorld.clearSession(engine);

        assertEquals(0, engine.getEntities().size(), "engine cleared by clearSession");
        Array<Body> remaining = new Array<>();
        world.getBodies(remaining);
        assertEquals(0, remaining.size, "world bodies cleared by clearSession");
    }

    @Test
    void clearSessionIsIdempotentOnEmptySession() {
        // First call on a fresh world — nothing to do.
        physicsWorld.clearSession(engine);
        assertEquals(0, engine.getEntities().size());

        // Second call must also be a clean no-op.
        physicsWorld.clearSession(engine);
        assertEquals(0, engine.getEntities().size());
    }

    @Test
    void clearSessionCanRunRepeatedlyWithRebuild() {
        World world = physicsWorld.getWorld();

        // Round 1.
        Body b1 = createBody(world);
        Entity e1 = new Entity();
        b1.setUserData(e1);
        engine.addEntity(e1);
        physicsWorld.clearSession(engine);

        // Round 2 — fresh bodies + entities should work after a clear.
        Body b2 = createBody(world);
        Entity e2 = new Entity();
        b2.setUserData(e2);
        engine.addEntity(e2);

        assertEquals(1, engine.getEntities().size());
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        assertEquals(1, bodies.size);

        physicsWorld.clearSession(engine);

        bodies.clear();
        world.getBodies(bodies);
        assertEquals(0, bodies.size);
        assertEquals(0, engine.getEntities().size());
    }

    private static Body createBody(World world) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        return world.createBody(def);
    }
}
