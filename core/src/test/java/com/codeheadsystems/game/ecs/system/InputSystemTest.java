package com.codeheadsystems.game.ecs.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InputSystemTest {

    private static final int SCREEN_WIDTH = 800;
    private static final int SPRITE_WIDTH = 96;
    private static final int SPRITE_HEIGHT = 128;
    private static final int MAX_X = SCREEN_WIDTH - SPRITE_WIDTH; // 704
    private static final float SPEED = 200f;
    private static final float PPM = 32f;
    private static final float SPEED_MPS = SPEED / PPM;
    private static final float DT = 0.016f;

    private Input input;
    private GameConfig config;
    private World world;
    private Body body;
    private PositionComponent position;
    private Engine engine;

    @BeforeAll
    static void loadNatives() {
        GdxNativesLoader.load();
        Box2D.init();
    }

    @BeforeEach
    void setUp() {
        input = mock(Input.class);
        Graphics graphics = mock(Graphics.class);
        when(graphics.getWidth()).thenReturn(SCREEN_WIDTH);

        config = new GameConfig();
        config.player = new GameConfig.PlayerConfig();
        config.player.speed = SPEED;
        config.physics = new GameConfig.PhysicsConfig();
        config.physics.pixelsPerMeter = PPM;

        world = new World(new Vector2(0f, 0f), true);

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.KinematicBody;
        body = world.createBody(def);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox((SPRITE_WIDTH / 2f) / PPM, (SPRITE_HEIGHT / 2f) / PPM);
        body.createFixture(shape, 0f);
        shape.dispose();

        position = new PositionComponent();
        TextureRegion region = mock(TextureRegion.class);
        when(region.getRegionWidth()).thenReturn(SPRITE_WIDTH);
        when(region.getRegionHeight()).thenReturn(SPRITE_HEIGHT);
        TextureComponent texture = new TextureComponent();
        texture.region = region;
        BodyComponent bc = new BodyComponent();
        bc.body = body;

        Entity player = new Entity();
        player.add(new InputComponent());
        player.add(position);
        player.add(texture);
        player.add(bc);

        engine = new PooledEngine();
        engine.addSystem(new InputSystem(input, graphics, config));
        engine.addEntity(player);
    }

    @AfterEach
    void tearDown() {
        world.dispose();
    }

    @Test
    void movesLeftWhenPointerIsLeftOfSprite() {
        placePlayerAt(400f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(100);

        engine.update(DT);

        assertEquals(-SPEED_MPS, body.getLinearVelocity().x, 1e-4);
    }

    @Test
    void movesRightWhenPointerIsRightOfSprite() {
        placePlayerAt(100f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(600);

        engine.update(DT);

        assertEquals(SPEED_MPS, body.getLinearVelocity().x, 1e-4);
    }

    @Test
    void stopsAndSnapsWhenAlreadyCenteredOnPointer() {
        // Sprite center = 100 + 48 = 148. Touching there means dx == 0 → snap path.
        placePlayerAt(100f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(148);

        engine.update(DT);

        assertEquals(0f, body.getLinearVelocity().x, 1e-4);
        assertEquals(148f / PPM, body.getPosition().x, 1e-4);
    }

    @Test
    void snapsToTargetWithinOneTickOfArrival() {
        // 1 px from the centered target; max step at SPEED*DT = 3.2 px, so we're inside the snap window.
        // Body must be teleported exactly to the target (not approximated through Box2D's stepper).
        placePlayerAt(100f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(149);

        engine.update(DT);

        assertEquals(0f, body.getLinearVelocity().x, 1e-4);
        assertEquals(149f / PPM, body.getPosition().x, 1e-4); // sprite center exactly under cursor
    }

    @Test
    void noTouchClearsVelocity() {
        body.setLinearVelocity(SPEED_MPS, 0f); // simulate mid-motion
        when(input.isTouched()).thenReturn(false);

        engine.update(DT);

        assertEquals(0f, body.getLinearVelocity().x, 1e-4);
    }

    @Test
    void clampsPositionWhenSpawnedOutOfBoundsLeft() {
        placePlayerAt(-50f);
        when(input.isTouched()).thenReturn(false);

        engine.update(DT);

        assertEquals(0f, position.x, 1e-4);
        // body center should be at (0 + 48) / PPM = 48 / PPM
        assertEquals(48f / PPM, body.getPosition().x, 1e-4);
    }

    @Test
    void doesNotOvershootRightEdgeEvenWhenPointerIsBeyondScreen() {
        // Integration with PhysicsSystem — tick until convergence; the sprite must stop exactly at maxX.
        engine.addSystem(new PhysicsSystem(world, config));
        placePlayerAt(700f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(2000); // far past the right edge

        for (int i = 0; i < 200; i++) {
            engine.update(DT);
        }

        assertEquals(MAX_X, position.x, 1e-3);
        assertEquals(0f, body.getLinearVelocity().x, 1e-3);
    }

    private void placePlayerAt(float xPx) {
        position.x = xPx;
        body.setTransform((xPx + SPRITE_WIDTH / 2f) / PPM, body.getPosition().y, 0f);
    }
}
