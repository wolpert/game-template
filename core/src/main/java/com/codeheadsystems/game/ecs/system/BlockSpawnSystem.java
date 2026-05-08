package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.EntitySystem;
import com.codeheadsystems.game.session.FallingBlockFactory;
import com.codeheadsystems.game.session.GameState;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Periodically spawns falling blocks while the session is alive; freezes on game over. */
@Singleton
public class BlockSpawnSystem extends EntitySystem {

    static final int PRIORITY = -9;
    static final float SPAWN_INTERVAL = 1.5f;

    private final FallingBlockFactory factory;
    private final GameState state;
    /** Initialized at the interval so the first {@link #update(float)} call spawns immediately. */
    private float timer = SPAWN_INTERVAL;

    @Inject
    public BlockSpawnSystem(FallingBlockFactory factory, GameState state) {
        super(PRIORITY);
        this.factory = factory;
        this.state = state;
    }

    @Override
    public void update(float deltaTime) {
        if (state.gameOver) return;
        timer += deltaTime;
        while (timer >= SPAWN_INTERVAL) {
            timer -= SPAWN_INTERVAL;
            getEngine().addEntity(factory.create());
        }
    }

    /** Reset the spawn cadence so the next update spawns immediately (used on session restart). */
    public void reset() {
        timer = SPAWN_INTERVAL;
    }
}
