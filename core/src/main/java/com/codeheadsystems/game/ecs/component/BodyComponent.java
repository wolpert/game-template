package com.codeheadsystems.game.ecs.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Pool;

public class BodyComponent implements Component, Pool.Poolable {
    public Body body;

    @Override
    public void reset() {
        // Body lifetime is owned by the World, not this component — destroyBody is the caller's job.
        body = null;
    }
}
