package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

/** Marker — identifies the player entity for systems that need to find it specifically (e.g., DeathSystem). */
public class PlayerComponent implements Component, Pool.Poolable {
    @Override
    public void reset() {
        // Pure marker — no state to clear.
    }
}
