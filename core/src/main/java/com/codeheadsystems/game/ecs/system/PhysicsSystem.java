package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Steps the Box2D world on a fixed timestep and writes each body's center position
 * back to its entity as the texture's bottom-left in pixels (the form RenderSystem expects).
 */
@Singleton
public class PhysicsSystem extends IteratingSystem {

    static final int PRIORITY = -8;
    static final float STEP_SECONDS = 1f / 60f;
    static final float MAX_FRAME_TIME = 0.25f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;

    private final ComponentMapper<BodyComponent> bodies = ComponentMapper.getFor(BodyComponent.class);
    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<TextureComponent> textures = ComponentMapper.getFor(TextureComponent.class);

    private final World world;
    private final float pixelsPerMeter;
    private float accumulator;

    @Inject
    public PhysicsSystem(World world, GameConfig config) {
        super(Family.all(BodyComponent.class, PositionComponent.class).get(), PRIORITY);
        this.world = world;
        this.pixelsPerMeter = config.physics.pixelsPerMeter;
    }

    @Override
    public void update(float deltaTime) {
        // Cap to avoid the spiral of death after a long pause / breakpoint.
        accumulator += Math.min(deltaTime, MAX_FRAME_TIME);
        while (accumulator >= STEP_SECONDS) {
            world.step(STEP_SECONDS, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= STEP_SECONDS;
        }
        super.update(deltaTime);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Body body = bodies.get(entity).body;
        Vector2 bp = body.getPosition();
        PositionComponent pos = positions.get(entity);

        // Box2D position is the body's center; RenderSystem draws from bottom-left, so subtract half-extents.
        TextureComponent tex = textures.get(entity);
        float halfW = (tex != null && tex.region != null) ? tex.region.getRegionWidth() / 2f : 0f;
        float halfH = (tex != null && tex.region != null) ? tex.region.getRegionHeight() / 2f : 0f;
        pos.x = bp.x * pixelsPerMeter - halfW;
        pos.y = bp.y * pixelsPerMeter - halfH;
    }
}
