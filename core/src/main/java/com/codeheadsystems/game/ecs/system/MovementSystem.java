package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.VelocityComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Plain Euler velocity integration: {@code pos += vel * dt}. The non-physical motion lane —
 * for scrolling backgrounds, drifting clouds, simple projectiles, anything that doesn't need
 * Box2D's solver. Runs after {@link PhysicsSystem} (priority {@value #PRIORITY} vs Physics's
 * -8) so that for a given frame, physics-driven entities have already had their positions
 * synced and only velocity-driven ones are integrated here.
 *
 * <p>Out of scope on purpose: no clamping, no wrap-around, no friction, no acceleration.
 * Add a separate system (see {@link WrapAroundSystem}) for any of those concerns rather than
 * folding them in here — that's why {@link IteratingSystem} priorities exist.
 *
 * <p>Demo consumer: the GameScreen background entity (the libGDX logo) is given a slow
 * {@link VelocityComponent} so it drifts horizontally; {@link WrapAroundSystem} (priority -4)
 * loops it back when it leaves the screen.
 */
@Singleton
public class MovementSystem extends IteratingSystem {

    static final int PRIORITY = -5;

    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<VelocityComponent> velocities = ComponentMapper.getFor(VelocityComponent.class);

    @Inject
    public MovementSystem() {
        super(Family.all(PositionComponent.class, VelocityComponent.class).get(), PRIORITY);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PositionComponent pos = positions.get(entity);
        VelocityComponent vel = velocities.get(entity);
        pos.x += vel.dx * deltaTime;
        pos.y += vel.dy * deltaTime;
    }
}
