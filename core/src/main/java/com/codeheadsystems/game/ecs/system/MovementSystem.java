package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.VelocityComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

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
