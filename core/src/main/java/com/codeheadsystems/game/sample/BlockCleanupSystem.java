package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Destroys block bodies and entities once they've fallen below the visible play area, and
 * enforces a hard cap on simultaneously-active blocks. Runs after {@link
 * com.codeheadsystems.game.ecs.system.PhysicsSystem} (priority -8) but well before {@link
 * com.codeheadsystems.game.ecs.system.RenderSystem} (priority 10) so the render pass never
 * sees a zombie body.
 *
 * <p>Without this system the demo's {@link World} fills indefinitely as
 * {@link BlockSpawnSystem} produces a new block every {@link BlockSpawnSystem#SPAWN_INTERVAL}
 * seconds — bodies that fall off-screen continue to be stepped by Box2D, which costs both
 * memory and CPU.
 */
@Singleton
public class BlockCleanupSystem extends EntitySystem {

    static final int PRIORITY = 5;
    /** Margin in pixels below screen y=0; blocks fully past this are removed. */
    static final float OFFSCREEN_MARGIN_PX = 200f;
    /** Hard cap on active blocks; the lowest-Y block is destroyed past this threshold. */
    static final int MAX_ACTIVE_BLOCKS = 50;

    private final ComponentMapper<BodyComponent> bodies = ComponentMapper.getFor(BodyComponent.class);

    private final World world;
    private final float pixelsPerMeter;
    private final Family family = Family.all(BlockComponent.class, BodyComponent.class).get();

    private ImmutableArray<Entity> entities;
    /**
     * Tracks entities destroyed within the current {@link #update(float)} call. Ashley defers
     * {@code engine.removeEntity()} while a system is updating, so the live family view still
     * reports already-removed entities — without this guard the hard-cap loop could pick the same
     * entity twice and double-destroy its body (Box2D crashes hard on that).
     */
    private final Set<Entity> destroyedThisFrame = new HashSet<>();

    @Inject
    public BlockCleanupSystem(World world, GameConfig config) {
        super(PRIORITY);
        this.world = world;
        this.pixelsPerMeter = config.physics.pixelsPerMeter;
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(family);
    }

    @Override
    public void removedFromEngine(Engine engine) {
        entities = null;
    }

    @Override
    public void update(float deltaTime) {
        if (entities == null || entities.size() == 0) return;
        try {
            // Pass 1: destroy anything that fell below the cleanup margin.
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                if (destroyedThisFrame.contains(entity)) continue;
                Body body = bodies.get(entity).body;
                if (body.getPosition().y * pixelsPerMeter < -OFFSCREEN_MARGIN_PX) {
                    destroy(entity, body);
                }
            }

            // Pass 2: hard cap. The family is a live view but Ashley *defers* removals during an
            // update, so size() doesn't shrink yet — we count alive entries manually.
            int alive = entities.size() - destroyedThisFrame.size();
            while (alive > MAX_ACTIVE_BLOCKS) {
                Entity lowest = null;
                float lowestY = Float.POSITIVE_INFINITY;
                for (int i = 0; i < entities.size(); i++) {
                    Entity e = entities.get(i);
                    if (destroyedThisFrame.contains(e)) continue;
                    float y = bodies.get(e).body.getPosition().y;
                    if (y < lowestY) {
                        lowestY = y;
                        lowest = e;
                    }
                }
                if (lowest == null) break; // defensive — shouldn't happen given alive > cap
                destroy(lowest, bodies.get(lowest).body);
                alive--;
            }
        } finally {
            destroyedThisFrame.clear();
        }
    }

    private void destroy(Entity entity, Body body) {
        world.destroyBody(body);
        getEngine().removeEntity(entity);
        destroyedThisFrame.add(entity);
    }
}
