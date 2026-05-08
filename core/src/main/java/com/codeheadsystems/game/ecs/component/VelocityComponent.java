package com.codeheadsystems.game.ecs.component;

import com.badlogic.ashley.core.Component;

/**
 * Linear velocity in pixels-per-second, integrated by
 * {@link com.codeheadsystems.game.ecs.system.MovementSystem} into a sibling
 * {@link PositionComponent}. Use this for purely cosmetic motion (scrolling backgrounds,
 * drifting decorations) where Box2D's collision/forces machinery is overkill — gameplay
 * entities that need physics use {@link BodyComponent} + the {@code PhysicsSystem} pipeline
 * instead. Velocity and Body together is undefined; pick one or the other.
 */
public class VelocityComponent implements Component {
    public float dx;
    public float dy;
}
