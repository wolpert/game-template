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
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import com.codeheadsystems.game.ecs.component.VelocityComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InputSystemTest {

    private static final int SCREEN_WIDTH = 800;
    private static final int SPRITE_WIDTH = 96;
    private static final int MAX_X = SCREEN_WIDTH - SPRITE_WIDTH; // 704
    private static final float SPEED = 200f;
    private static final float DT = 0.016f;

    private Input input;
    private Graphics graphics;
    private GameConfig config;
    private PositionComponent position;
    private VelocityComponent velocity;
    private Engine engine;

    @BeforeEach
    void setUp() {
        input = mock(Input.class);
        graphics = mock(Graphics.class);
        when(graphics.getWidth()).thenReturn(SCREEN_WIDTH);

        config = new GameConfig();
        config.player = new GameConfig.PlayerConfig();
        config.player.speed = SPEED;

        position = new PositionComponent();
        velocity = new VelocityComponent();
        TextureComponent texture = new TextureComponent();
        TextureRegion region = mock(TextureRegion.class);
        when(region.getRegionWidth()).thenReturn(SPRITE_WIDTH);
        texture.region = region;

        Entity player = new Entity();
        player.add(new InputComponent());
        player.add(position);
        player.add(velocity);
        player.add(texture);

        engine = new PooledEngine();
        engine.addSystem(new InputSystem(input, graphics, config));
        engine.addEntity(player);
    }

    @Test
    void movesLeftWhenPointerIsLeftOfSprite() {
        position.x = 400f;
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(100);

        engine.update(DT);

        assertEquals(-SPEED, velocity.dx);
    }

    @Test
    void movesRightWhenPointerIsRightOfSprite() {
        position.x = 100f;
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(600);

        engine.update(DT);

        assertEquals(SPEED, velocity.dx);
    }

    @Test
    void stopsWhenAlreadyCenteredOnPointer() {
        // Sprite center = position.x + SPRITE_WIDTH/2 = 100 + 48 = 148. Touching there means dx == 0.
        position.x = 100f;
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(148);

        engine.update(DT);

        assertEquals(0f, velocity.dx);
    }

    @Test
    void calibratesFinalTickSoSpriteLandsOnTargetWithoutOvershoot() {
        // 1 px from the centered target; max step at SPEED*DT = 3.2 px, so we're inside the snap window.
        // Velocity must be calibrated so MovementSystem (next priority) lands exactly on target.
        position.x = 100f;
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(149);

        engine.update(DT);

        // MovementSystem will do pos.x += velocity.dx * DT; verify the result would be exactly 101.
        float projected = position.x + velocity.dx * DT;
        assertEquals(101f, projected, 1e-4);
    }

    @Test
    void noTouchClearsVelocity() {
        velocity.dx = SPEED; // simulate mid-motion
        when(input.isTouched()).thenReturn(false);

        engine.update(DT);

        assertEquals(0f, velocity.dx);
    }

    @Test
    void clampsPositionWhenSpawnedOutOfBounds() {
        position.x = -50f; // off-screen left
        when(input.isTouched()).thenReturn(false);

        engine.update(DT);

        assertEquals(0f, position.x);
    }

    @Test
    void doesNotOvershootRightEdgeEvenWhenPointerIsBeyondScreen() {
        // Integration with MovementSystem — tick until convergence; the sprite must stop exactly at maxX.
        engine.addSystem(new MovementSystem());
        position.x = 700f;
        when(input.isTouched()).thenReturn(true);
        when(input.getX()).thenReturn(2000); // far past the right edge

        for (int i = 0; i < 200; i++) {
            engine.update(DT);
        }

        assertEquals(MAX_X, position.x, 1e-3);
        assertEquals(0f, velocity.dx, 1e-3);
    }
}
