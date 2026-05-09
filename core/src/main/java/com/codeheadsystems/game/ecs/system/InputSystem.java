package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.physics.box2d.Body;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.InputGate;
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
 *
 * <p><b>Platform divergence (touch vs. cursor).</b> On desktop the cursor is always present, so
 * centering the sprite on the pointer feels natural. On Android the pointer only exists while
 * a finger is down, and the first touch can land anywhere — so cursor-centered tracking would
 * teleport the player on first contact. To fix that, Android uses <em>drag-anchor</em> mode:
 * the first touch is recorded as an anchor (both pointer X and current sprite center) and the
 * target tracks pointer-X minus that anchor delta, so only the drag distance moves the player.
 * The anchor is cleared on touch-up so each new gesture starts fresh. Per the C5 contradiction
 * resolution in TODO.md, the platform check is gated on {@link ApplicationType#Android} rather
 * than rebuilt as a per-platform subclass — keeps the wiring single-source.
 */
@Singleton
public class InputSystem extends IteratingSystem {

    static final int PRIORITY = -10;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<TextureComponent> textures = ComponentMapper.getFor(TextureComponent.class);
    private final ComponentMapper<BodyComponent> bodies = ComponentMapper.getFor(BodyComponent.class);
    private final Input input;
    private final Graphics graphics;
    private final InputGate inputGate;
    private final float speedPx;
    private final float pixelsPerMeter;
    private final boolean useDragAnchor;

    // Drag-anchor state (only consulted when useDragAnchor == true). Captured on touch-down,
    // cleared on touch-up — singleton system holds it across frames intentionally.
    private boolean anchored;
    private float anchorPointerX;
    private float anchorSpriteCenterX;

    @Inject
    public InputSystem(Input input, Graphics graphics, GameConfig config, InputGate inputGate) {
        this(input, graphics, config, inputGate, isAndroid());
    }

    /** Package-private for tests so we can drive both platform branches without spinning up libGDX. */
    InputSystem(Input input, Graphics graphics, GameConfig config, InputGate inputGate, boolean useDragAnchor) {
        super(Family.all(InputComponent.class, BodyComponent.class,
                PositionComponent.class, TextureComponent.class).get(), PRIORITY);
        this.input = input;
        this.graphics = graphics;
        this.inputGate = inputGate;
        this.speedPx = config.player.speed;
        this.pixelsPerMeter = config.physics.pixelsPerMeter;
        this.useDragAnchor = useDragAnchor;
    }

    private static boolean isAndroid() {
        Application app = Gdx.app;
        return app != null && app.getType() == ApplicationType.Android;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Body body = bodies.get(entity).body;
        if (!inputGate.isInputActive()) {
            // Gate closed — freeze input-driven entities (e.g. during DYING / GAME_OVER in the demo) so
            // they aren't dragged around by the cursor. Default scaffold binding returns true always;
            // sample modules rebind to a gameplay-state-aware gate.
            body.setLinearVelocity(0f, 0f);
            return;
        }
        PositionComponent pos = positions.get(entity);
        TextureComponent tex = textures.get(entity);

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
            // Touch-up: clear the anchor so the next gesture starts fresh from wherever the finger lands.
            anchored = false;
            return;
        }

        float pointerX = input.getX();
        float targetX;
        if (useDragAnchor) {
            // Android: first touch records pointer + current sprite-center as the anchor and the sprite
            // does NOT move this tick — preventing the teleport-on-first-tap bug. Subsequent ticks
            // translate the sprite by (pointerX − anchorPointerX), so only drag distance matters.
            if (!anchored) {
                anchored = true;
                anchorPointerX = pointerX;
                anchorSpriteCenterX = pos.x + spriteW / 2f;
            }
            float desiredCenterX = anchorSpriteCenterX + (pointerX - anchorPointerX);
            targetX = clamp(desiredCenterX - spriteW / 2f, 0f, maxX);
        } else {
            // Desktop: cursor-centered tracking — sprite chases the pointer continuously.
            targetX = clamp(pointerX - spriteW / 2f, 0f, maxX);
        }
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
