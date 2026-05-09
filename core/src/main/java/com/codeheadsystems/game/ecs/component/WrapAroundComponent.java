package com.codeheadsystems.game.ecs.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

/**
 * Marks an entity as horizontally looping: when its left edge passes the right edge of the screen
 * (or vice versa), {@link com.codeheadsystems.game.ecs.system.WrapAroundSystem} respawns it on
 * the opposite side. {@link #widthPx} is the entity's own sprite width — we can't read it from
 * {@link TextureComponent} without coupling the wrap system to the texture loader, so the spawner
 * sets it explicitly.
 */
public class WrapAroundComponent implements Component, Pool.Poolable {
    public float widthPx;

    @Override
    public void reset() {
        widthPx = 0f;
    }
}
