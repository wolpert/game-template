package com.codeheadsystems.game.sample;

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
import com.codeheadsystems.game.ecs.InputGate;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import com.codeheadsystems.game.ecs.system.PhysicsSystem;
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
    private Graphics graphics;
    private InputGate gate;
    private GameConfig config;
    private boolean inputActive;
    private World world;
    private Body body;
    private PositionComponent position;
    private Entity player;
    private Engine engine;

    @BeforeAll
    static void loadNatives() {
        GdxNativesLoader.load();
        Box2D.init();
    }

    @BeforeEach
    void setUp() {
        input = mock(Input.class);
        graphics = mock(Graphics.class);
        when(graphics.getWidth()).thenReturn(SCREEN_WIDTH);

        config = new GameConfig();
        config.player = new GameConfig.PlayerConfig();
        config.player.speed = SPEED;
        config.physics = new GameConfig.PhysicsConfig();
        config.physics.pixelsPerMeter = PPM;

        inputActive = true;
        gate = () -> inputActive;

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

        player = new Entity();
        player.add(new InputComponent());
        player.add(position);
        player.add(texture);
        player.add(bc);

        engine = new PooledEngine();
        engine.addSystem(new InputSystem(input, graphics, config, gate));
        engine.addEntity(player);
    }

    /** Replace the desktop-mode system with the Android (drag-anchor) variant. */
    private void useAndroidMode() {
        for (com.badlogic.ashley.core.EntitySystem s : engine.getSystems().toArray(com.badlogic.ashley.core.EntitySystem.class)) {
            if (s instanceof InputSystem) {
                engine.removeSystem(s);
            }
        }
        engine.addSystem(new InputSystem(input, graphics, config, gate, /*useDragAnchor=*/ true));
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
    void freezesBodyWhenInputGateClosed() {
        // Gate-closed (e.g. DYING / GAME_OVER in the demo) should ignore the cursor and zero velocity each tick.
        inputActive = false;
        body.setLinearVelocity(SPEED_MPS, 0f); // simulate mid-motion at the moment the gate closes
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(50);

        engine.update(DT);

        assertEquals(0f, body.getLinearVelocity().x, 1e-4);
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

    // -- Android drag-anchor mode --------------------------------------------------------------
    // Regression suite for the "first touch teleports player" bug. On Android the first touch
    // anchors to the current sprite position; only drag deltas move the player.

    @Test
    void androidFirstTouchDoesNotTeleport() {
        // Player sits at x=400; finger lands at x=100 (≈sprite center 148). Pre-fix, the cursor-centered
        // path would immediately drive left toward x=100. Drag-anchor mode must keep the sprite still on
        // the first frame of contact (zero drag distance → zero target delta).
        useAndroidMode();
        placePlayerAt(400f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(100);

        engine.update(DT);

        assertEquals(0f, body.getLinearVelocity().x, 1e-4);
        assertEquals(400f, position.x, 1e-4);
    }

    @Test
    void androidDragDeltaMovesPlayer() {
        // Anchor on first tick, drag right 200 px on the second — sprite chases the drag delta.
        useAndroidMode();
        placePlayerAt(400f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(100); // anchor frame
        engine.update(DT);

        when(input.getX()).thenReturn(300); // dragged +200 px
        engine.update(DT);

        // Sprite center was 448 at anchor → desired center now 648 → target bottom-left 600. 600 > 400 → move right.
        assertEquals(SPEED_MPS, body.getLinearVelocity().x, 1e-4);
    }

    @Test
    void androidDragDeltaMovesPlayerLeft() {
        useAndroidMode();
        placePlayerAt(400f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(500); // anchor
        engine.update(DT);

        when(input.getX()).thenReturn(300); // dragged -200 px
        engine.update(DT);

        assertEquals(-SPEED_MPS, body.getLinearVelocity().x, 1e-4);
    }

    @Test
    void androidAnchorClearsOnTouchUp() {
        // Anchor → release → re-touch elsewhere should NOT teleport: a fresh anchor must be captured.
        useAndroidMode();
        placePlayerAt(400f);
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(100);
        engine.update(DT); // anchor at pointer=100, sprite center=448

        when(input.isTouched()).thenReturn(false);
        engine.update(DT); // release → clears anchor

        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(700); // far away — pre-clear this would have been treated as huge drag
        engine.update(DT); // first tick of new gesture: anchors fresh, no movement

        assertEquals(0f, body.getLinearVelocity().x, 1e-4);
        assertEquals(400f, position.x, 1e-4);
    }

    private void placePlayerAt(float xPx) {
        position.x = xPx;
        body.setTransform((xPx + SPRITE_WIDTH / 2f) / PPM, body.getPosition().y, 0f);
    }
}
