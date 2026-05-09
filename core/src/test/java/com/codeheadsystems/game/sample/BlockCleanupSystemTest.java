package com.codeheadsystems.game.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockCleanupSystemTest {

    private static final float PPM = 32f;

    private World world;
    private Engine engine;

    @BeforeAll
    static void loadNatives() {
        GdxNativesLoader.load();
        Box2D.init();
    }

    @BeforeEach
    void setUp() {
        GameConfig config = new GameConfig();
        config.physics = new GameConfig.PhysicsConfig();
        config.physics.pixelsPerMeter = PPM;
        world = new World(new Vector2(0f, 0f), true);
        engine = new PooledEngine();
        engine.addSystem(new BlockCleanupSystem(world, config));
    }

    @AfterEach
    void tearDown() {
        world.dispose();
    }

    @Test
    void destroysBlockBelowOffscreenMargin() {
        // Place a block well below the cleanup margin (-200px / PPM ≈ -6.25 m). Use -10 m to be safe.
        Entity block = makeBlock(0f, -10f);
        engine.addEntity(block);

        engine.update(0.016f);

        ImmutableArray<Entity> remaining = engine.getEntitiesFor(
                Family.all(BlockComponent.class, BodyComponent.class).get());
        assertEquals(0, remaining.size(), "off-screen block should have been removed");
        assertEquals(0, world.getBodyCount(), "world body should have been destroyed");
    }

    @Test
    void leavesOnscreenBlockAlone() {
        Entity block = makeBlock(0f, 5f); // well above the cleanup margin
        engine.addEntity(block);

        engine.update(0.016f);

        ImmutableArray<Entity> remaining = engine.getEntitiesFor(
                Family.all(BlockComponent.class, BodyComponent.class).get());
        assertEquals(1, remaining.size());
        assertEquals(1, world.getBodyCount());
    }

    @Test
    void enforcesHardCap() {
        // Add MAX + 5 blocks all on-screen so the offscreen pass is a no-op; the cap pass should
        // trim back to exactly MAX by destroying the lowest-Y blocks.
        for (int i = 0; i < BlockCleanupSystem.MAX_ACTIVE_BLOCKS + 5; i++) {
            // y values 0..N-1 (in meters). Lowest-Y = the first 5 added.
            engine.addEntity(makeBlock(0f, i * 0.1f));
        }

        engine.update(0.016f);

        ImmutableArray<Entity> remaining = engine.getEntitiesFor(
                Family.all(BlockComponent.class, BodyComponent.class).get());
        assertEquals(BlockCleanupSystem.MAX_ACTIVE_BLOCKS, remaining.size());
        assertEquals(BlockCleanupSystem.MAX_ACTIVE_BLOCKS, world.getBodyCount());
        // The 5 lowest blocks (y=0, 0.1, 0.2, 0.3, 0.4) should be the ones destroyed.
        for (int i = 0; i < remaining.size(); i++) {
            Body b = remaining.get(i).getComponent(BodyComponent.class).body;
            assertTrue(b.getPosition().y >= 0.5f - 1e-4,
                    "lowest blocks should have been destroyed, but found y=" + b.getPosition().y);
        }
    }

    private Entity makeBlock(float xMeters, float yMeters) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(xMeters, yMeters);
        Body body = world.createBody(def);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 0.5f);
        body.createFixture(shape, 1f);
        shape.dispose();

        Entity e = new Entity();
        e.add(new BlockComponent());
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        e.add(bc);
        return e;
    }
}
