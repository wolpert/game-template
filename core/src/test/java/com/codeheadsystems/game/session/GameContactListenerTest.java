package com.codeheadsystems.game.session;

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
import com.codeheadsystems.game.ecs.component.BlockComponent;
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
    void setsGameOverWhenPlayerCollidesWithBlock() {
        Entity playerEntity = new Entity();
        playerEntity.add(new InputComponent());
        Body player = createUnitBody(0f, 0f);
        player.setUserData(playerEntity);

        Entity blockEntity = new Entity();
        blockEntity.add(new BlockComponent());
        // Overlap by more than the slop tolerance so beginContact fires immediately.
        Body block = createUnitBody(0.5f, 0f);
        block.setUserData(blockEntity);

        for (int i = 0; i < 10 && !state.gameOver; i++) {
            world.step(1f / 60f, 6, 2);
        }

        assertTrue(state.gameOver);
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

        assertFalse(state.gameOver);
    }

    @Test
    void ignoresBodiesWithoutEntityUserData() {
        // Two bodies with no userData (e.g., walls or ground) overlap — listener must not crash or trip game-over.
        createUnitBody(0f, 0f);
        createUnitBody(0.5f, 0f);

        for (int i = 0; i < 10; i++) {
            world.step(1f / 60f, 6, 2);
        }

        assertFalse(state.gameOver);
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
}
