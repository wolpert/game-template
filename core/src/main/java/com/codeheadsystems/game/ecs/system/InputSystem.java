package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.physics.box2d.Body;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Drives input-controlled physics bodies toward the pointer at constant speed,
 * stopping at the pointer or the screen edge — whichever comes first.
 * Works for desktop mouse and mobile touch since libGDX abstracts both behind {@link Input}.
 */
@Singleton
public class InputSystem extends IteratingSystem {

    static final int PRIORITY = -10;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<TextureComponent> textures = ComponentMapper.getFor(TextureComponent.class);
    private final ComponentMapper<BodyComponent> bodies = ComponentMapper.getFor(BodyComponent.class);
    private final Input input;
    private final Graphics graphics;
    private final float speedPx;
    private final float pixelsPerMeter;

    @Inject
    public InputSystem(Input input, Graphics graphics, GameConfig config) {
        super(Family.all(InputComponent.class, BodyComponent.class,
                PositionComponent.class, TextureComponent.class).get(), PRIORITY);
        this.input = input;
        this.graphics = graphics;
        this.speedPx = config.player.speed;
        this.pixelsPerMeter = config.physics.pixelsPerMeter;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PositionComponent pos = positions.get(entity);
        TextureComponent tex = textures.get(entity);
        Body body = bodies.get(entity).body;

        int spriteW = tex.region.getRegionWidth();
        float maxX = Math.max(0f, graphics.getWidth() - spriteW);

        // Hold the bounds invariant — covers initial spawn and window-resize-shrink.
        if (pos.x < 0f || pos.x > maxX) {
            float corrected = clamp(pos.x, 0f, maxX);
            snapBody(body, corrected, spriteW);
            pos.x = corrected;
        }

        if (!input.isTouched()) {
            body.setLinearVelocity(0f, 0f);
            return;
        }

        // Center the sprite on the pointer, kept on-screen.
        float targetX = clamp(input.getX() - spriteW / 2f, 0f, maxX);
        float dx = targetX - pos.x;

        if (Math.abs(dx) <= speedPx * deltaTime) {
            // Within one frame's max travel: snap and stop. setTransform is the right tool for kinematic
            // bodies and avoids the overshoot you'd get from the Box2D fixed timestep not aligning with dt.
            body.setLinearVelocity(0f, 0f);
            snapBody(body, targetX, spriteW);
        } else {
            body.setLinearVelocity(Math.signum(dx) * speedPx / pixelsPerMeter, 0f);
        }
    }

    private void snapBody(Body body, float spriteBottomLeftX, int spriteW) {
        body.setTransform((spriteBottomLeftX + spriteW / 2f) / pixelsPerMeter,
                body.getPosition().y, body.getAngle());
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
