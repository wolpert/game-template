package com.codeheadsystems.game.ecs.component;

import com.badlogic.ashley.core.Component;

public class PositionComponent implements Component {
    public float x;
    public float y;
    /** Render layer; lower values draw first (further back). */
    public int z;
    /** Rotation in degrees, applied around the texture's center by RenderSystem. */
    public float angle;
}
