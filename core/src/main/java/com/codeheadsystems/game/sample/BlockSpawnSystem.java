package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Graphics;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Periodically spawns falling blocks while the session is alive; freezes on game over. */
@Singleton
public class BlockSpawnSystem extends EntitySystem {

    static final int PRIORITY = -9;
    static final float SPAWN_INTERVAL = 1.5f;

    private final FallingBlockFactory factory;
    private final GameState state;
    private final Graphics graphics;
    /** Initialized at the interval so the first {@link #update(float)} call spawns immediately. */
    private float timer = SPAWN_INTERVAL;

    @Inject
    public BlockSpawnSystem(FallingBlockFactory factory, GameState state, Graphics graphics) {
        super(PRIORITY);
        this.factory = factory;
        this.state = state;
        this.graphics = graphics;
    }

    @Override
    public void update(float deltaTime) {
        if (!state.isPlaying()) return;
        timer += deltaTime;
        if (timer < SPAWN_INTERVAL) return;
        // Snapshot screen dims once per frame instead of once per spawn — getWidth/Height are JNI
        // hops on Android. Spawns share the same dims (same frame), so this is correct, not just faster.
        float screenW = graphics.getWidth();
        float screenH = graphics.getHeight();
        while (timer >= SPAWN_INTERVAL) {
            timer -= SPAWN_INTERVAL;
            getEngine().addEntity(factory.create(screenW, screenH));
        }
    }

    /** Reset the spawn cadence so the next update spawns immediately (used on session restart). */
    public void reset() {
        timer = SPAWN_INTERVAL;
    }
}
