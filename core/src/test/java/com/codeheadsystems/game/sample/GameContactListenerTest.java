package com.codeheadsystems.game.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.codeheadsystems.game.ecs.component.InputComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameContactListenerTest {

    private GameState state;
    private World world;

    @BeforeAll
    static void loadNatives() {
        GdxNativesLoader.load();
        Box2D.init();
    }

    @BeforeEach
    void setUp() {
        state = new GameState();
        world = new World(new Vector2(0f, 0f), true);
        world.setContactListener(new GameContactListener(state));
    }

    @AfterEach
    void tearDown() {
        world.dispose();
    }

    @Test
    void decrementsHpOnPlayerBlockHitButStaysInPlaying() {
        spawnPlayerAt(0f, 0f);
        spawnBlockAt(0.5f, 0f); // overlapping with player

        stepUntil(() -> state.hp < GameState.MAX_HP, 10);

        assertEquals(GameState.MAX_HP - 1, state.hp);
        assertEquals(GameState.Phase.PLAYING, state.phase);
    }

    @Test
    void transitionsToDyingAfterHpReachesZero() {
        spawnPlayerAt(0f, 0f);
        // Each block contributes one beginContact event (one hit) before they all rest in a pile.
        for (int i = 0; i < GameState.MAX_HP; i++) {
            spawnBlockAt(0.4f + i * 0.1f, 0f); // tightly packed, all overlapping the player
        }

        stepUntil(() -> state.phase == GameState.Phase.DYING, 30);

        assertEquals(0, state.hp);
        assertEquals(GameState.Phase.DYING, state.phase);
    }

    @Test
    void doesNotDecrementWhenAlreadyDying() {
        state.phase = GameState.Phase.DYING;
        state.hp = 0;
        spawnPlayerAt(0f, 0f);
        spawnBlockAt(0.5f, 0f);

        for (int i = 0; i < 10; i++) {
            world.step(1f / 60f, 6, 2);
        }

        assertEquals(0, state.hp);
        assertEquals(GameState.Phase.DYING, state.phase);
    }

    @Test
    void doesNotSetGameOverForBlockBlockCollisions() {
        Entity blockA = new Entity();
        blockA.add(new BlockComponent());
        Body bodyA = createUnitBody(0f, 0f);
        bodyA.setUserData(blockA);

        Entity blockB = new Entity();
        blockB.add(new BlockComponent());
        Body bodyB = createUnitBody(0.5f, 0f);
        bodyB.setUserData(blockB);

        for (int i = 0; i < 10; i++) {
            world.step(1f / 60f, 6, 2);
        }

        assertEquals(GameState.MAX_HP, state.hp);
        assertFalse(state.isGameOver());
    }

    @Test
    void ignoresBodiesWithoutEntityUserData() {
        createUnitBody(0f, 0f);
        createUnitBody(0.5f, 0f);

        for (int i = 0; i < 10; i++) {
            world.step(1f / 60f, 6, 2);
        }

        assertEquals(GameState.MAX_HP, state.hp);
        assertEquals(GameState.Phase.PLAYING, state.phase);
    }

    private void spawnPlayerAt(float x, float y) {
        Entity player = new Entity();
        player.add(new InputComponent());
        Body body = createUnitBody(x, y);
        body.setUserData(player);
    }

    private void spawnBlockAt(float x, float y) {
        Entity block = new Entity();
        block.add(new BlockComponent());
        Body body = createUnitBody(x, y);
        body.setUserData(block);
    }

    private Body createUnitBody(float x, float y) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x, y);
        Body body = world.createBody(def);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 0.5f);
        body.createFixture(shape, 1f);
        shape.dispose();
        return body;
    }

    private void stepUntil(java.util.function.BooleanSupplier condition, int maxSteps) {
        for (int i = 0; i < maxSteps; i++) {
            world.step(1f / 60f, 6, 2);
            if (condition.getAsBoolean()) {
                assertTrue(true);
                return;
            }
        }
        assertTrue(condition.getAsBoolean(), "condition never became true within " + maxSteps + " steps");
    }
}
