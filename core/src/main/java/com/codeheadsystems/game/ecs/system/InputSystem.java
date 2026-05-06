package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import com.codeheadsystems.game.ecs.component.VelocityComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Drives input-controlled entities toward the pointer at constant speed,
 * stopping when they reach it and never crossing the screen edges.
 * Works for desktop mouse and mobile touch since libGDX abstracts both behind {@link Input}.
 */
@Singleton
public class InputSystem extends IteratingSystem {

    static final int PRIORITY = -10;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);
    private final ComponentMapper<TextureComponent> textures = ComponentMapper.getFor(TextureComponent.class);
    private final Input input;
    private final Graphics graphics;
    private final float speed;

    @Inject
    public InputSystem(Input input, Graphics graphics, GameConfig config) {
        super(Family.all(InputComponent.class, VelocityComponent.class,
                PositionComponent.class, TextureComponent.class).get(), PRIORITY);
        this.input = input;
        this.graphics = graphics;
        this.speed = config.player.speed;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PositionComponent pos = positions.get(entity);
        VelocityComponent vel = velocities.get(entity);
        TextureComponent tex = textures.get(entity);

        int spriteW = tex.region.getRegionWidth();
        float maxX = Math.max(0f, graphics.getWidth() - spriteW);

        // Hold the bounds invariant — covers initial spawn and window-resize-shrink.
        pos.x = clamp(pos.x, 0f, maxX);

        if (!input.isTouched()) {
            vel.dx = 0f;
            return;
        }

        // Center the sprite on the pointer, kept on-screen.
        float targetX = clamp(input.getX() - spriteW / 2f, 0f, maxX);
        float dx = targetX - pos.x;

        if (Math.abs(dx) <= speed * deltaTime) {
            // Within one tick of arrival: calibrate velocity to land exactly on the target,
            // so MovementSystem doesn't overshoot. Stops the sprite at the cursor without snap.
            vel.dx = (deltaTime > 0f) ? dx / deltaTime : 0f;
        } else {
            vel.dx = Math.signum(dx) * speed;
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
