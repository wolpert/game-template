package com.codeheadsystems.game.ecs.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

/**
 * Linear velocity in pixels-per-second, integrated by
 * {@link com.codeheadsystems.game.ecs.system.MovementSystem} into a sibling
 * {@link PositionComponent}. Use this for purely cosmetic motion (scrolling backgrounds,
 * drifting decorations) where Box2D's collision/forces machinery is overkill — gameplay
 * entities that need physics use {@link BodyComponent} + the {@code PhysicsSystem} pipeline
 * instead. Velocity and Body together is undefined; pick one or the other.
 */
public class VelocityComponent implements Component, Pool.Poolable {
    public float dx;
    public float dy;

    @Override
    public void reset() {
        dx = 0f;
        dy = 0f;
    }
}
