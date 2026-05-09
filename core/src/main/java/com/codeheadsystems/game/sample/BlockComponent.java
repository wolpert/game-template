package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;

/** Marker — used by the contact listener to identify falling-block entities. */
public class BlockComponent implements Component, Pool.Poolable {
    @Override
    public void reset() {
        // Pure marker — no state to clear.
    }
}
