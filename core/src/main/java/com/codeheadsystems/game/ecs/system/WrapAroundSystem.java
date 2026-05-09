package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Graphics;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.WrapAroundComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Loops drifting entities horizontally so they re-enter on the opposite side once fully off-screen.
 * Runs at priority {@value #PRIORITY} — after {@link MovementSystem} (-5), before
 * {@link AnimationSystem} / {@link RenderSystem} — so a given frame: integrate velocity, wrap, draw.
 *
 * <p>Vertical wrap isn't implemented; gameplay y-axis collision (ground edge, falling blocks) makes
 * vertical looping nonsensical here. Add a {@code wrapY} flag to {@link WrapAroundComponent} if a
 * future demo needs it.
 */
@Singleton
public class WrapAroundSystem extends IteratingSystem {

    static final int PRIORITY = -4;

    private final Graphics graphics;
    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<WrapAroundComponent> wraps = ComponentMapper.getFor(WrapAroundComponent.class);

    /** Cached at the start of each frame to avoid per-entity JNI hops on Android. */
    private float screenW;

    @Inject
    public WrapAroundSystem(Graphics graphics) {
        super(Family.all(PositionComponent.class, WrapAroundComponent.class).get(), PRIORITY);
        this.graphics = graphics;
    }

    @Override
    public void update(float deltaTime) {
        // Snapshot once per frame — getWidth() is a JNI call on Android. Always reflects the
        // current size, so this picks up resizes the next frame for free.
        screenW = graphics.getWidth();
        super.update(deltaTime);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PositionComponent pos = positions.get(entity);
        float widthPx = wraps.get(entity).widthPx;
        // pos.x is bottom-left in pixels; wrap when fully past either edge.
        if (pos.x > screenW) {
            pos.x = -widthPx;
        } else if (pos.x + widthPx < 0f) {
            pos.x = screenW;
        }
    }
}
