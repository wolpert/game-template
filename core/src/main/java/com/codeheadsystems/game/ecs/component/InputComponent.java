package com.codeheadsystems.game.ecs.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

/** Marker: this entity's velocity is driven by the player's pointer input. */
public class InputComponent implements Component, Pool.Poolable {
    @Override
    public void reset() {
        // Pure marker — no state to clear.
    }
}
